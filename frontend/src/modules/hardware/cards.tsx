import { ComponentCard } from '../../components/ComponentCard'
import { FieldList } from '../../components/FieldList'
import { UsageBar } from '../../components/UsageBar'
import { bytes, count, duration, frequency, NOT_AVAILABLE, NOT_REPORTED, text } from './format'
import type { Inventory, MotherboardField } from './types'

type Result<K extends keyof Inventory> = Inventory[K]

export function OsCard({ result }: { result: Result<'os'> }) {
  return (
    <ComponentCard title="Sistema operacional" result={result}>
      {(os) => (
        <FieldList
          fields={[
            ['Nome', text(os.name)],
            ['Versão', text(os.version)],
            ['Build', text(os.buildNumber)],
            ['Arquitetura', text(os.architecture)],
            ['Tempo de atividade', duration(os.uptimeSeconds)],
          ]}
        />
      )}
    </ComponentCard>
  )
}

export function CpuCard({ result }: { result: Result<'cpu'> }) {
  return (
    <ComponentCard title="CPU" result={result}>
      {(cpu) => (
        <>
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
          <UsageBar label="Uso atual" value={cpu.usagePercent} />
          <p className="muted">O uso de CPU é uma medição curta no momento da coleta e varia a cada atualização.</p>
        </>
      )}
    </ComponentCard>
  )
}

export function MemoryCard({ result }: { result: Result<'memory'> }) {
  return (
    <ComponentCard title="Memória RAM" result={result}>
      {(memory) => (
        <>
          <FieldList
            fields={[
              ['Total', bytes(memory.totalBytes)],
              ['Usada', bytes(memory.usedBytes)],
              ['Disponível', bytes(memory.availableBytes)],
            ]}
          />
          <UsageBar label="Utilização" value={memory.usedPercent} />
        </>
      )}
    </ComponentCard>
  )
}

export function GpuCard({ result }: { result: Result<'gpu'> }) {
  return (
    <ComponentCard title="Placa de vídeo" result={result}>
      {(gpus) => (
        <>
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

export function MotherboardCard({ result }: { result: Result<'motherboard'> }) {
  return (
    <ComponentCard title="Placa-mãe" result={result}>
      {(board) => {
        const field = (name: MotherboardField) =>
          board[name] == null && board.notReportedFields?.includes(name) ? NOT_REPORTED : text(board[name])
        return (
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
        )
      }}
    </ComponentCard>
  )
}

export function StorageCard({ result }: { result: Result<'storage'> }) {
  return (
    <ComponentCard title="Armazenamento" result={result}>
      {(storage) => (
        <>
          <h3>Unidades físicas</h3>
          {storage.drives === null ? (
            <p className="muted">{NOT_AVAILABLE}</p>
          ) : (
            storage.drives.map((drive, index) => (
              <div className="subitem" key={`${drive.name ?? 'drive'}-${index}`}>
                <FieldList
                  fields={[
                    ['Nome', text(drive.name)],
                    ['Modelo', text(drive.model)],
                    ['Capacidade', bytes(drive.sizeBytes)],
                  ]}
                />
              </div>
            ))
          )}

          <h3>Volumes</h3>
          {storage.volumes === null ? (
            <p className="muted">{NOT_AVAILABLE}</p>
          ) : (
            storage.volumes.map((volume, index) => (
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
            ))
          )}
          <p className="muted">O tipo da unidade (SSD/HDD) não é informado por não haver evidência confiável.</p>
        </>
      )}
    </ComponentCard>
  )
}
