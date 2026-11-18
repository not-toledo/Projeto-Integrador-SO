import { ComponentCard } from '../../components/ComponentCard'
import { FieldList } from '../../components/FieldList'
import { Spot } from '../../components/Spot'
import { UsageBar } from '../../components/UsageBar'
import { bytes, count, duration, frequency, NOT_AVAILABLE, NOT_REPORTED, percent, text } from './format'
import { DataSources } from '../sources/DataSources'
import type { Platform } from '../sources/sourceCatalog'
import type { Inventory, MotherboardField, VirtualMemoryInfo } from './types'

type Result<K extends keyof Inventory> = Inventory[K]
/** platform: SO deste computador, para destacar a coluna correspondente em "De onde vêm estes dados?". */
type CardProps<K extends keyof Inventory> = { result: Result<K>; platform?: Platform | null }

export function OsCard({ result, platform }: CardProps<'os'>) {
  return (
    <ComponentCard title="Sistema operacional" result={result} footer={<DataSources section="os" platform={platform} />}>
      {(os) => (
        <>
        <Spot
          value={[os.name, os.version].filter(Boolean).join(' ') || NOT_AVAILABLE}
          missing={!os.name}
          caption={[os.buildNumber && `Build ${os.buildNumber}`, os.architecture,
            os.uptimeSeconds != null && `ligado há ${duration(os.uptimeSeconds)}`].filter(Boolean).join(' · ')}
        />
        <FieldList
          fields={[
            ['Nome', text(os.name)],
            ['Versão', text(os.version)],
            ['Build', text(os.buildNumber)],
            ['Arquitetura', text(os.architecture)],
            ['Tempo de atividade', duration(os.uptimeSeconds)],
          ]}
        />
        </>
      )}
    </ComponentCard>
  )
}

export function CpuCard({ result, platform }: CardProps<'cpu'>) {
  return (
    <ComponentCard title="CPU" result={result} footer={<DataSources section="cpu" platform={platform} />}>
      {(cpu) => (
        <>
          <Spot
            value={cpu.physicalCores != null && cpu.logicalProcessors != null
              ? `${cpu.physicalCores} núcleos · ${cpu.logicalProcessors} threads` : NOT_AVAILABLE}
            missing={cpu.physicalCores == null || cpu.logicalProcessors == null}
            caption={cpu.nominalFrequencyHz != null ? `Frequência nominal de ${frequency(cpu.nominalFrequencyHz)}` : undefined}
            gauge={{ label: 'Uso no momento da coleta', percent: cpu.usagePercent, text: percent(cpu.usagePercent) }}
          />
          <FieldList
            fields={[
              ['Modelo', text(cpu.name)],
              ['Fabricante', text(cpu.vendor)],
              ['Núcleos físicos', count(cpu.physicalCores)],
              ['Processadores lógicos', count(cpu.logicalProcessors)],
              ['Frequência nominal', frequency(cpu.nominalFrequencyHz)],
              ['Frequência máxima', frequency(cpu.maxFrequencyHz)],
            ]}
          />
          <p className="muted">O uso de CPU é uma medição curta no momento da coleta e varia a cada atualização.</p>
        </>
      )}
    </ComponentCard>
  )
}

function swapText(vm: VirtualMemoryInfo | null | undefined): string {
  if (!vm || vm.swapTotalBytes == null) return NOT_AVAILABLE
  if (vm.swapTotalBytes === 0) return 'Desativado'
  return `${bytes(vm.swapUsedBytes)} em uso de ${bytes(vm.swapTotalBytes)}`
}

/** Memória reservada pelos programas e o limite (RAM + paginação). Pode passar de 100% no Linux (overcommit). */
function commitText(vm: VirtualMemoryInfo | null | undefined): string {
  if (!vm || vm.commitUsedBytes == null || vm.commitLimitBytes == null) return NOT_AVAILABLE
  const percent = vm.commitPercent == null ? ''
    : ` (${vm.commitPercent.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%)`
  return `${bytes(vm.commitUsedBytes)} de ${bytes(vm.commitLimitBytes)}${percent}`
}

export function MemoryCard({ result, platform }: CardProps<'memory'>) {
  return (
    <ComponentCard title="Memória RAM" result={result} footer={<DataSources section="memory" platform={platform} />}>
      {(memory) => (
        <>
          <Spot
            value={memory.usedPercent != null ? `${percent(memory.usedPercent)} em uso` : NOT_AVAILABLE}
            missing={memory.usedPercent == null}
            caption={memory.usedBytes != null && memory.totalBytes != null
              ? `${bytes(memory.usedBytes)} usados de ${bytes(memory.totalBytes)}` : undefined}
            gauge={{ label: 'Utilização', percent: memory.usedPercent, text: percent(memory.usedPercent) }}
          />
          <FieldList
            fields={[
              ['Total', bytes(memory.totalBytes)],
              ['Usada', bytes(memory.usedBytes)],
              ['Disponível', bytes(memory.availableBytes)],
              ['Arquivo de paginação / swap', swapText(memory.virtualMemory)],
              ['Memória comprometida', commitText(memory.virtualMemory)],
            ]}
          />
        </>
      )}
    </ComponentCard>
  )
}

export function GpuCard({ result, platform }: CardProps<'gpu'>) {
  return (
    <ComponentCard title="Placa de vídeo" result={result} footer={<DataSources section="gpu" platform={platform} />}>
      {(gpus) => (
        <>
          {gpus.length > 0 && (
            <Spot
              value={gpus.length === 1 && gpus[0].vramBytes != null ? `${bytes(gpus[0].vramBytes)} de VRAM`
                : `${gpus.length} ${gpus.length === 1 ? 'placa' : 'placas'} de vídeo`}
              caption={`${gpus.length === 1 ? '1 placa' : `${gpus.length} placas`}: ${gpus.map((g) => text(g.name)).join(' + ')}`}
            />
          )}
          {gpus.map((gpu, index) => (
            <div className="subitem" key={`${gpu.name ?? 'gpu'}-${index}`}>
              {gpus.length > 1 && <h3>Placa {index + 1}</h3>}
              <FieldList
                fields={[
                  ['Nome', text(gpu.name)],
                  ['Fabricante', text(gpu.vendor)],
                  ['ID do dispositivo', text(gpu.deviceId)],
                  ['VRAM informada pelo sistema', bytes(gpu.vramBytes)],
                ]}
              />
            </div>
          ))}
          <p className="muted">
            A utilização e a temperatura da GPU aparecem na aba Live Monitor (via leitor de sensores do PCCheck). A VRAM
            informada aqui não distingue memória dedicada de compartilhada.
          </p>
        </>
      )}
    </ComponentCard>
  )
}

export function MotherboardCard({ result, platform }: CardProps<'motherboard'>) {
  return (
    <ComponentCard title="Placa-mãe" result={result} footer={<DataSources section="motherboard" platform={platform} />}>
      {(board) => {
        const field = (name: MotherboardField) =>
          board[name] == null && board.notReportedFields?.includes(name) ? NOT_REPORTED : text(board[name])
        return (
          <>
          <Spot
            value={board.model ? `Modelo ${board.model}` : 'Modelo não informado'}
            missing={!board.model}
            caption={board.manufacturer ? `Fabricante: ${board.manufacturer}` : undefined}
          />
          <FieldList
            fields={[
              ['Fabricante', field('manufacturer')],
              ['Modelo', field('model')],
              ['Versão', field('version')],
              ['Número de série', field('serialNumber')],
              ['Computador (fabricante)', field('systemManufacturer')],
              ['Computador (modelo)', field('systemModel')],
            ]}
          />
          </>
        )
      }}
    </ComponentCard>
  )
}

/** "SSD (SATA)", "HDD", "USB" ou "Não disponível": somente o que o SO informou. */
function diskType(media: string | null | undefined, bus: string | null | undefined): string {
  if (media && bus) return `${media} (${bus})`
  return media ?? bus ?? NOT_AVAILABLE
}

export function StorageCard({ result, platform }: CardProps<'storage'>) {
  return (
    <ComponentCard title="Armazenamento" result={result} footer={<DataSources section="storage" platform={platform} />} wide>
      {(storage) => (
        <>
          {storage.drives && storage.drives.length > 0 && (
            <Spot
              value={`${storage.drives.length} ${storage.drives.length === 1 ? 'disco' : 'discos'} · ${bytes(
                storage.drives.reduce((sum, d) => sum + (d.sizeBytes ?? 0), 0))}`}
              caption={storage.drives.map((d) => diskType(d.mediaType, d.busType)).join(' · ')}
            />
          )}
          <h3>Unidades físicas</h3>
          {storage.drives === null ? (
            <p className="muted">{NOT_AVAILABLE}</p>
          ) : (
            <div className="subgrid">
            {storage.drives.map((drive, index) => (
              <div className="subitem" key={`${drive.name ?? 'drive'}-${index}`}>
                <FieldList
                  fields={[
                    ['Nome', text(drive.name)],
                    ['Modelo', text(drive.model)],
                    ['Capacidade', bytes(drive.sizeBytes)],
                    ['Tipo', diskType(drive.mediaType, drive.busType)],
                    ['Volumes neste disco', drive.mountPoints?.length ? drive.mountPoints.join(', ') : NOT_AVAILABLE],
                  ]}
                />
              </div>
            ))}
            </div>
          )}

          <h3>Volumes</h3>
          {storage.volumes === null ? (
            <p className="muted">{NOT_AVAILABLE}</p>
          ) : (
            <div className="subgrid">
            {storage.volumes.map((volume, index) => (
              <div className="subitem" key={`${volume.mountPoint ?? 'volume'}-${index}`}>
                <FieldList
                  fields={[
                    ['Ponto de montagem', text(volume.mountPoint)],
                    ['Rótulo', text(volume.label)],
                    ['Sistema de arquivos', text(volume.fileSystem)],
                    ['Capacidade', bytes(volume.totalBytes)],
                    ['Livre', bytes(volume.freeBytes)],
                    ['Usado', bytes(volume.usedBytes)],
                  ]}
                />
                <UsageBar label="Utilização" value={volume.usedPercent} />
              </div>
            ))}
            </div>
          )}
          <p className="muted">O tipo (SSD/HDD) aparece somente quando o sistema operacional o informa; nunca é deduzido do nome do modelo.</p>
        </>
      )}
    </ComponentCard>
  )
}
