import type { ReactNode } from 'react'

export type Field = [label: string, value: ReactNode]

/** Lista de pares rótulo/valor. O chamador já passa os valores formatados ("Não disponível" quando ausentes). */
export function FieldList({ fields }: { fields: Field[] }) {
  return (
    <dl className="fields">
      {fields.map(([label, value]) => (
        <div className="field" key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  )
}
