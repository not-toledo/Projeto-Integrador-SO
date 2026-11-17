using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.IO.Pipes;
using System.Security.AccessControl;
using System.Security.Principal;
using System.Text;
using System.Threading;
using LibreHardwareMonitor.Hardware;
using Microsoft.Win32;

namespace PCCheck.SensorHost
{
    /// <summary>
    /// Leitor de sensores do PCCheck. Iniciado pelo backend Java com:
    ///   PCCheck.SensorHost.exe --pipe pccheck-sensors-&lt;id&gt; --parent &lt;pid do backend&gt;
    ///
    /// A cada conexão no pipe nomeado, escreve UMA linha JSON com os sensores e desconecta. O pipe é somente
    /// de saída: o cliente apenas lê, então não há comandos a interpretar. Nenhuma porta de rede é aberta.
    /// O processo termina sozinho quando o backend (--parent) termina.
    ///
    /// Os valores são enviados como números (cultura invariável); a escolha de qual sensor representa a
    /// "temperatura da CPU" etc. fica no Java (SensorHostParser), onde é testada.
    /// </summary>
    internal static class Program
    {
        private const string PipePrefix = "pccheck-sensors-";
        private const int MinRefreshMs = 500;

        private static int Main(string[] args)
        {
            string pipeName = Arg(args, "--pipe");
            string parentArg = Arg(args, "--parent");
            if (pipeName == null || !pipeName.StartsWith(PipePrefix, StringComparison.Ordinal) || pipeName.Length > 128
                || parentArg == null || !int.TryParse(parentArg, NumberStyles.None, CultureInfo.InvariantCulture, out int parentPid))
            {
                return 2;
            }
            if (!WatchParent(parentPid))
            {
                return 3;
            }

            bool elevated = IsElevated();
            bool driver = IsPawnIoInstalled();
            var computer = new Computer
            {
                IsCpuEnabled = true,
                IsGpuEnabled = true,
                IsMotherboardEnabled = true,
                IsControllerEnabled = true,
            };
            string openError = null;
            try
            {
                computer.Open();
            }
            catch (Exception e)
            {
                openError = e.GetType().Name + ": " + e.Message;
            }

            NamedPipeServerStream server;
            try
            {
                server = new NamedPipeServerStream(pipeName, PipeDirection.Out, 1, PipeTransmissionMode.Byte,
                    PipeOptions.None, 0, 256 * 1024, CreatePipeSecurity());
            }
            catch (Exception)
            {
                // Nome já em uso (outra instância) ou sem permissão: não há como servir os dados.
                return 4;
            }

            string cached = null;
            long cachedAt = 0;
            var clock = Stopwatch.StartNew();
            using (server)
            {
                while (true)
                {
                    try
                    {
                        server.WaitForConnection();
                        if (cached == null || clock.ElapsedMilliseconds - cachedAt >= MinRefreshMs)
                        {
                            cached = Snapshot(computer, elevated, driver, openError);
                            cachedAt = clock.ElapsedMilliseconds;
                        }
                        byte[] data = Encoding.UTF8.GetBytes(cached + "\n");
                        server.Write(data, 0, data.Length);
                        server.Flush();
                        server.WaitForPipeDrain();
                    }
                    catch (IOException)
                    {
                        // Cliente desistiu (tempo limite) ou fechou antes de ler tudo: atende o próximo.
                    }
                    finally
                    {
                        try
                        {
                            server.Disconnect();
                        }
                        catch (Exception)
                        {
                            // já desconectado
                        }
                    }
                }
            }
        }

        private static string Arg(string[] args, string name)
        {
            for (int i = 0; i + 1 < args.Length; i++)
            {
                if (args[i] == name)
                {
                    return args[i + 1];
                }
            }
            return null;
        }

        /// <summary>Encerra este processo quando o backend terminar (inclusive se for fechado à força).</summary>
        private static bool WatchParent(int pid)
        {
            Process parent;
            try
            {
                parent = Process.GetProcessById(pid);
            }
            catch (ArgumentException)
            {
                return false;
            }
            var watcher = new Thread(() =>
            {
                try
                {
                    parent.WaitForExit();
                }
                catch (Exception)
                {
                    // sem acesso ao processo: encerra por segurança
                }
                Environment.Exit(0);
            })
            {
                IsBackground = true,
                Name = "parent-watcher",
            };
            watcher.Start();
            return true;
        }

        /// <summary>
        /// Leitura para usuários autenticados (o backend roda sem elevação); controle total só para quem criou.
        /// Os dados são leituras de sensores, sem informação sensível.
        /// </summary>
        private static PipeSecurity CreatePipeSecurity()
        {
            var security = new PipeSecurity();
            SecurityIdentifier owner = WindowsIdentity.GetCurrent().User;
            if (owner != null)
            {
                security.AddAccessRule(new PipeAccessRule(owner, PipeAccessRights.FullControl, AccessControlType.Allow));
            }
            security.AddAccessRule(new PipeAccessRule(
                new SecurityIdentifier(WellKnownSidType.AuthenticatedUserSid, null),
                PipeAccessRights.Read | PipeAccessRights.Synchronize, AccessControlType.Allow));
            return security;
        }

        private static bool IsElevated()
        {
            using (WindowsIdentity identity = WindowsIdentity.GetCurrent())
            {
                return new WindowsPrincipal(identity).IsInRole(WindowsBuiltInRole.Administrator);
            }
        }

        /// <summary>O driver PawnIO é o que permite ler a temperatura da CPU e as ventoinhas da placa-mãe.</summary>
        private static bool IsPawnIoInstalled()
        {
            try
            {
                using (RegistryKey key = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Services\PawnIO"))
                {
                    return key != null;
                }
            }
            catch (Exception)
            {
                return false;
            }
        }

        private static string Snapshot(Computer computer, bool elevated, bool driver, string openError)
        {
            var sb = new StringBuilder(8192);
            sb.Append("{\"version\":1,\"elevated\":").Append(elevated ? "true" : "false")
              .Append(",\"driver\":").Append(driver ? "true" : "false")
              .Append(",\"error\":").Append(openError == null ? "null" : Str(openError))
              .Append(",\"hardware\":[");
            bool first = true;
            if (openError == null)
            {
                foreach (IHardware hardware in computer.Hardware)
                {
                    AppendHardware(sb, hardware, ref first);
                }
            }
            sb.Append("]}");
            return sb.ToString();
        }

        /// <summary>Sub-hardware (ex.: chip Super I/O da placa-mãe) vira uma entrada própria na lista.</summary>
        private static void AppendHardware(StringBuilder sb, IHardware hardware, ref bool first)
        {
            try
            {
                hardware.Update();
            }
            catch (Exception)
            {
                return;
            }
            if (!first)
            {
                sb.Append(',');
            }
            first = false;
            sb.Append("{\"type\":").Append(Str(hardware.HardwareType.ToString()))
              .Append(",\"name\":").Append(Str(hardware.Name))
              .Append(",\"sensors\":[");
            bool firstSensor = true;
            foreach (ISensor sensor in hardware.Sensors)
            {
                float? value = sensor.Value;
                if (!value.HasValue || float.IsNaN(value.Value) || float.IsInfinity(value.Value))
                {
                    continue;
                }
                if (!firstSensor)
                {
                    sb.Append(',');
                }
                firstSensor = false;
                sb.Append("{\"type\":").Append(Str(sensor.SensorType.ToString()))
                  .Append(",\"name\":").Append(Str(sensor.Name))
                  .Append(",\"value\":").Append(value.Value.ToString("R", CultureInfo.InvariantCulture))
                  .Append('}');
            }
            sb.Append("]}");
            foreach (IHardware sub in hardware.SubHardware)
            {
                AppendHardware(sb, sub, ref first);
            }
        }

        private static string Str(string text)
        {
            if (text == null)
            {
                return "null";
            }
            var sb = new StringBuilder(text.Length + 2);
            sb.Append('"');
            foreach (char c in text)
            {
                switch (c)
                {
                    case '"': sb.Append("\\\""); break;
                    case '\\': sb.Append("\\\\"); break;
                    case '\n': sb.Append("\\n"); break;
                    case '\r': sb.Append("\\r"); break;
                    case '\t': sb.Append("\\t"); break;
                    default:
                        if (c < 0x20)
                        {
                            sb.Append("\\u").Append(((int)c).ToString("x4", CultureInfo.InvariantCulture));
                        }
                        else
                        {
                            sb.Append(c);
                        }
                        break;
                }
            }
            sb.Append('"');
            return sb.ToString();
        }
    }
}
