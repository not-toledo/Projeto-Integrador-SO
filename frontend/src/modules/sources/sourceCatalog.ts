// De onde vem cada dado exibido pelo PCCheck, no Windows e no Linux.
//
// As fontes foram conferidas no código do OSHI 6.6.5 (a biblioteca de coleta do agente), no leitor de sensores
// (sensor-host) e no próprio agente. "OSHI" é só o intermediário: a origem é sempre uma interface do sistema
// operacional (registro, WMI, contadores de desempenho, /proc, /sys ou um comando do sistema).

export type Platform = 'windows' | 'linux'

export interface DataSource {
  /** Rótulo exatamente como aparece na interface. */
  field: string
  windows: string
  linux: string
}

export type SourceSection =
  | 'os' | 'cpu' | 'memory' | 'gpu' | 'motherboard' | 'storage'
  | 'live-cpu' | 'live-memory' | 'live-gpu' | 'live-sensors' | 'live-disks'

const SENSOR_HOST = 'Leitor de sensores do PCCheck (sensor-host, LibreHardwareMonitorLib)'

const MEMORY_COMMON: DataSource[] = [
  { field: 'Total', windows: 'API GetPerformanceInfo (Psapi): PhysicalTotal', linux: '/proc/meminfo: MemTotal' },
  { field: 'Disponível', windows: 'API GetPerformanceInfo (Psapi): PhysicalAvailable', linux: '/proc/meminfo: MemAvailable' },
  { field: 'Usada', windows: 'Calculado: total − disponível', linux: 'Calculado: total − disponível' },
  {
    field: 'Memória comprometida',
    windows: 'API GetPerformanceInfo (Psapi): CommitTotal e CommitLimit',
    linux: '/proc/meminfo: Committed_AS e CommitLimit (lido pelo agente)',
  },
]

export const DATA_SOURCES: Record<SourceSection, DataSource[]> = {
  os: [
    { field: 'Nome', windows: 'Propriedade os.name do Java (fornecida pelo Windows)', linux: '/etc/os-release (ou /etc/lsb-release)' },
    { field: 'Versão', windows: 'WMI Win32_OperatingSystem', linux: '/etc/os-release' },
    { field: 'Build', windows: 'WMI Win32_OperatingSystem: BuildNumber', linux: 'Versão do kernel (/proc/version)' },
    {
      field: 'Arquitetura',
      windows: 'Propriedade os.arch da máquina virtual Java (arquitetura do Java em execução)',
      linux: 'Propriedade os.arch da máquina virtual Java (arquitetura do Java em execução)',
    },
    { field: 'Tempo de atividade', windows: 'API GetTickCount64 (Kernel32)', linux: '/proc/uptime' },
  ],
  cpu: [
    {
      field: 'Modelo',
      windows: 'Registro: HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0 (ProcessorNameString)',
      linux: '/proc/cpuinfo: model name',
    },
    { field: 'Fabricante', windows: 'Registro: CentralProcessor\\0 (VendorIdentifier)', linux: '/proc/cpuinfo: vendor_id' },
    {
      field: 'Núcleos físicos',
      windows: 'API GetLogicalProcessorInformationEx (Kernel32)',
      linux: '/sys/devices/system/cpu/cpu*/topology (core_id, physical_package_id)',
    },
    {
      field: 'Processadores lógicos',
      windows: 'API GetLogicalProcessorInformationEx (Kernel32)',
      linux: '/sys/devices/system/cpu e /proc/cpuinfo',
    },
    { field: 'Frequência nominal', windows: 'Registro: CentralProcessor\\0 (~MHz)', linux: '/proc/cpuinfo (frequência citada no nome do modelo)' },
    {
      field: 'Frequência máxima',
      windows: 'API CallNtPowerInformation (PowrProf)',
      linux: '/sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq',
    },
    {
      field: 'Uso atual',
      windows: 'API GetSystemTimes (Kernel32), diferença em 0,5 s',
      linux: '/proc/stat (tempo de CPU por estado; diferença em 0,5 s)',
    },
  ],
  memory: [
    ...MEMORY_COMMON,
    {
      field: 'Arquivo de paginação / swap',
      windows: 'Tamanho: GetPerformanceInfo (limite − RAM); uso: contador "Paging File % Usage"',
      linux: '/proc/meminfo: SwapTotal e SwapFree',
    },
  ],
  gpu: [
    {
      field: 'Nome',
      windows: 'Registro: classe de adaptadores de vídeo {4d36e968-…} (DriverDesc); WMI Win32_VideoController',
      linux: 'Comandos lspci -vnnm e lshw -C display',
    },
    { field: 'Fabricante', windows: 'Registro (ProviderName); WMI Win32_VideoController', linux: 'Comandos lspci e lshw' },
    { field: 'ID do dispositivo', windows: 'WMI Win32_VideoController (DeviceID)', linux: 'Comando lspci (identificador PCI)' },
    {
      field: 'VRAM informada pelo sistema',
      windows: 'Registro (HardwareInformation.qwMemorySize); WMI Win32_VideoController',
      linux: 'Comando lspci (regiões de memória; pode não refletir a VRAM real)',
    },
  ],
  motherboard: [
    { field: 'Fabricante', windows: 'WMI Win32_BaseBoard: Manufacturer', linux: '/sys/devices/virtual/dmi/id/board_vendor' },
    {
      field: 'Modelo',
      windows: 'WMI Win32_BaseBoard: Product (lido pelo agente; o OSHI lê Model)',
      linux: '/sys/devices/virtual/dmi/id/board_name',
    },
    { field: 'Versão', windows: 'WMI Win32_BaseBoard: Version', linux: '/sys/devices/virtual/dmi/id/board_version' },
    {
      field: 'Número de série',
      windows: 'WMI Win32_BaseBoard: SerialNumber',
      linux: '/sys/devices/virtual/dmi/id/board_serial (exige root)',
    },
    { field: 'Computador (fabricante)', windows: 'WMI Win32_ComputerSystem: Manufacturer', linux: '/sys/devices/virtual/dmi/id/sys_vendor' },
    { field: 'Computador (modelo)', windows: 'WMI Win32_ComputerSystem: Model', linux: '/sys/devices/virtual/dmi/id/product_name' },
  ],
  storage: [
    { field: 'Nome', windows: 'WMI Win32_DiskDrive (\\\\.\\PHYSICALDRIVEn)', linux: 'udev (/sys/block, /dev/sdX, /dev/nvmeXnY)' },
    { field: 'Modelo', windows: 'WMI Win32_DiskDrive: Model', linux: 'udev (atributos do dispositivo)' },
    { field: 'Capacidade', windows: 'WMI Win32_DiskDrive (disco); GetDiskFreeSpaceEx (volume)', linux: 'udev (disco); statvfs (volume)' },
    {
      field: 'Tipo',
      windows: 'WMI MSFT_PhysicalDisk (ROOT\\Microsoft\\Windows\\Storage): MediaType e BusType (lido pelo agente)',
      linux: '/sys/block/<disco>/queue/rotational e /sys/block/<disco>/removable (lido pelo agente)',
    },
    {
      field: 'Volumes neste disco',
      windows: 'WMI Win32_DiskDriveToDiskPartition e Win32_LogicalDiskToPartition (partição → letra da unidade)',
      linux: 'udev (partições do disco) e /proc/mounts (ponto de montagem)',
    },
    { field: 'Ponto de montagem', windows: 'APIs FindFirstVolume e GetVolumePathNamesForVolumeName (Kernel32)', linux: '/proc/mounts' },
    { field: 'Rótulo', windows: 'API GetVolumeInformation (Kernel32)', linux: 'Comando lsblk -o mountpoint,label' },
    { field: 'Sistema de arquivos', windows: 'API GetVolumeInformation (Kernel32)', linux: '/proc/mounts' },
    { field: 'Livre', windows: 'API GetDiskFreeSpaceEx (Kernel32)', linux: 'Chamada statvfs (LibC)' },
    { field: 'Usado', windows: 'Calculado: capacidade − livre', linux: 'Calculado: capacidade − livre' },
  ],
  'live-cpu': [
    {
      field: 'Uso total e por núcleo',
      windows: 'Total: API GetSystemTimes (Kernel32); por núcleo: contadores "Processor Information" (diferença entre amostras)',
      linux: '/proc/stat (diferença entre amostras)',
    },
  ],
  'live-memory': [
    ...MEMORY_COMMON,
    {
      field: 'Paginação (gravação em disco)',
      windows: 'Contador "Memory\\Pages Output/sec" (diferença entre amostras; inclui arquivos mapeados)',
      linux: '/proc/vmstat: pswpout (diferença entre amostras; só swap)',
    },
  ],
  'live-gpu': [
    { field: 'Placa', windows: SENSOR_HOST, linux: 'Não suportado no Linux nesta versão' },
    { field: 'Utilização', windows: `${SENSOR_HOST}, via driver da placa`, linux: 'Não suportado no Linux nesta versão' },
    { field: 'Memória de vídeo em uso', windows: `${SENSOR_HOST}, via driver da placa`, linux: 'Não suportado no Linux nesta versão' },
    { field: 'Memória de vídeo total', windows: `${SENSOR_HOST}, via driver da placa`, linux: 'Não suportado no Linux nesta versão' },
    { field: 'Temperatura', windows: `${SENSOR_HOST}, via driver da placa`, linux: 'Não suportado no Linux nesta versão' },
  ],
  'live-sensors': [
    {
      field: 'Temperatura da CPU',
      windows: `${SENSOR_HOST}; exige administrador e o driver PawnIO`,
      linux: '/sys/class/hwmon e /sys/class/thermal (OSHI)',
    },
    { field: 'Temperatura da GPU', windows: `${SENSOR_HOST}, via driver da placa`, linux: 'Não suportado no Linux nesta versão' },
    {
      field: 'Ventoinhas',
      windows: `${SENSOR_HOST}; exige administrador e o driver PawnIO`,
      linux: '/sys/class/hwmon: fan*_input (OSHI)',
    },
  ],
  'live-disks': [
    {
      field: 'Leitura, escrita e tempo ocupado',
      windows: 'Contadores de desempenho "PhysicalDisk" (diferença entre amostras)',
      linux: '/proc/diskstats (diferença entre amostras)',
    },
    { field: 'Volumes', windows: 'APIs GetDiskFreeSpaceEx e GetVolumeInformation (Kernel32)', linux: '/proc/mounts e statvfs' },
  ],
}

/** Plataforma a partir do nome do SO informado pelo inventário; null se desconhecida. */
export function platformOf(osName: string | null | undefined): Platform | null {
  const name = (osName ?? '').toLowerCase()
  if (name.includes('windows')) return 'windows'
  if (name.includes('linux')) return 'linux'
  return null
}
