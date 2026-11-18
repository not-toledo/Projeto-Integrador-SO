import { useSyncExternalStore } from 'react'

/** Telas do PCCheck. O endereço usa o fragmento (#/live) para o botão Voltar do navegador funcionar sem biblioteca de rotas. */
export type View = 'home' | 'hardware' | 'health' | 'live' | 'processes' | 'history' | 'games'

/** Telas do menu principal. O Histórico (coleta agendada) continua acessível pelo rodapé e por #/history. */
export const VIEWS: { id: View; label: string }[] = [
  { id: 'home', label: 'Início' },
  { id: 'hardware', label: 'Hardware' },
  { id: 'health', label: 'Saúde' },
  { id: 'live', label: 'Monitor' },
  { id: 'processes', label: 'Processos' },
  { id: 'games', label: 'Jogos' },
]

const IDS = new Set<string>([...VIEWS.map((v) => v.id), 'history'])

export function hrefOf(view: View): string {
  return view === 'home' ? '#/' : `#/${view}`
}

/** "#/live" → "live". Fragmento vazio ou desconhecido abre o Início. */
export function viewFromHash(hash: string): View {
  const id = hash.replace(/^#\/?/, '').split(/[/?]/)[0]
  return IDS.has(id) ? (id as View) : 'home'
}

function subscribe(onChange: () => void) {
  window.addEventListener('hashchange', onChange)
  return () => window.removeEventListener('hashchange', onChange)
}

export function useView(): View {
  return useSyncExternalStore(subscribe, () => viewFromHash(window.location.hash), () => 'home')
}
