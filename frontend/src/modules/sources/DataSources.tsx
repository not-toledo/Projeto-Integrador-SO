import { DATA_SOURCES, type Platform, type SourceSection } from './sourceCatalog'

const PLATFORM_LABEL: Record<Platform, string> = { windows: 'Windows', linux: 'Linux' }

/**
 * "De onde vêm estes dados?": a interface do sistema operacional que fornece cada campo, no Windows e no Linux.
 * A coluna do SO deste computador é destacada quando ele é conhecido.
 */
export function DataSources({ section, platform }: { section: SourceSection; platform?: Platform | null }) {
  const rows = DATA_SOURCES[section]
  const columns: Platform[] = ['windows', 'linux']
  return (
    <details className="data-sources">
      <summary>De onde vêm estes dados?</summary>
      {platform && <p className="muted">Este computador usa {PLATFORM_LABEL[platform]}: a coluna correspondente está destacada.</p>}
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th scope="col">Campo</th>
              {columns.map((p) => (
                <th scope="col" key={p} className={p === platform ? 'data-sources-current' : undefined}>
                  {PLATFORM_LABEL[p]}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.field}>
                <th scope="row">{row.field}</th>
                {columns.map((p) => (
                  <td key={p} className={p === platform ? 'data-sources-current' : undefined}>{row[p]}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  )
}
