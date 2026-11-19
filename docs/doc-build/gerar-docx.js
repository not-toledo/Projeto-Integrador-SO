/*
 * Gera docs/NEXCORE-documentacao.docx a partir de docs/NEXCORE-documentacao.md, no modelo de trabalho acadêmico da
 * FATEC (ABNT): capa, folha de rosto, resumo, abstract, listas, sumário, capítulos, referências, apêndice e anexo.
 *
 *   node gerar-docx.js <arquivo.md> <saida.docx> [paginas.json]
 *
 * paginas.json (opcional) traz a página de cada título e legenda, medida no PDF da primeira passagem
 * (veja gerar.ps1): sem ele, os números de página das listas e do sumário saem como "00".
 * Diagramas Mermaid viram PNG com o mermaid-cli (mmdc), usando o Edge instalado (pp.json).
 */
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');
const {
  AlignmentType, BorderStyle, Document, Footer, FootnoteReferenceRun, Header, ImageRun, LevelFormat, Packer,
  PageBreak, PageNumber, Paragraph, ShadingType, Table, TableCell, TableRow, TabStopType, TextRun, VerticalAlign,
  WidthType, LineRuleType,
} = require('docx');

const [mdPath, outPath, pagesPath] = process.argv.slice(2);
const mdDir = path.dirname(path.resolve(mdPath));
const pages = pagesPath && fs.existsSync(pagesPath) ? JSON.parse(fs.readFileSync(pagesPath, 'utf8')) : {};
const cacheDir = path.join(__dirname, '.cache');
fs.mkdirSync(cacheDir, { recursive: true });

// ---------------------------------------------------------------- dados da capa (modelo da FATEC)
const CURSO = 'ANÁLISE E DESENVOLVIMENTO DE SISTEMAS';
const NOMES = ['GABRIEL', 'KARL', 'MARCOS', 'ENZO', 'VINICIUS'];
const TITULO = 'NEXCORE: aplicação de conceitos de Sistemas Operacionais no monitoramento e diagnóstico de hardware';
const NATUREZA = 'Trabalho apresentado à FATEC Itapetininga como instrumento de avaliação da disciplina de Sistemas ' +
  'Operacionais.';
const ORIENTADOR = 'Orientador (a): Deivison S. Takatu';
const LOCAL = 'ITAPETININGA, SP';
const ANO = '2026';
const ABREVIATURAS = [['Ex.', 'Exemplo'], ['Máx.', 'Máximo']];
const SIGLAS = [
  ['ABNT', 'Associação Brasileira de Normas Técnicas'], ['API', 'Interface de Programação de Aplicações'],
  ['CPU', 'Unidade Central de Processamento'], ['CSS', 'Folhas de Estilo em Cascata'],
  ['FATEC', 'Faculdade de Tecnologia'], ['FPS', 'Quadros por Segundo'], ['GPU', 'Unidade de Processamento Gráfico'],
  ['HDD', 'Disco Rígido'], ['HTTP', 'Protocolo de Transferência de Hipertexto'],
  ['HTTPS', 'Protocolo de Transferência de Hipertexto Seguro'], ['IPC', 'Comunicação entre Processos'],
  ['JDK', 'Kit de Desenvolvimento Java'], ['JSON', 'Notação de Objetos JavaScript'],
  ['PID', 'Identificador de Processo'], ['RAM', 'Memória de Acesso Aleatório'],
  ['SMART', 'Tecnologia de Automonitoramento, Análise e Relatório'], ['SO', 'Sistema Operacional'],
  ['SSD', 'Unidade de Estado Sólido'], ['SVG', 'Gráficos Vetoriais Escaláveis'],
  ['UAC', 'Controle de Conta de Usuário'], ['USB', 'Barramento Serial Universal'],
  ['WCAG', 'Diretrizes de Acessibilidade para Conteúdo Web'], ['WMI', 'Instrumentação de Gerenciamento do Windows'],
];

// ---------------------------------------------------------------- formatação (modelo / ABNT)
const FONT = 'Arial';
const CM = 567; // twips por centímetro
const TEXT_WIDTH = 16 * CM; // A4 (21 cm) - 3 cm - 2 cm
const LINE_15 = { line: 360, lineRule: LineRuleType.AUTO };
const LINE_1 = { line: 240, lineRule: LineRuleType.AUTO };
const run = (text, o = {}) => new TextRun({ text, font: o.font || FONT, size: o.size || 24, bold: o.bold, italics: o.italics, color: o.color });

/** **negrito**, *itálico*, `código` e [^n] (nota de rodapé). */
function inline(text, o = {}) {
  const out = [];
  const re = /(\*\*(?:[^*]|\*(?!\*))+\*\*|`[^`]+`|\*[^*]+\*|\[\^\d+\])/g;
  let last = 0;
  let m;
  while ((m = re.exec(text))) {
    if (m.index > last) out.push(run(text.slice(last, m.index), o));
    const t = m[0];
    if (t.startsWith('**')) out.push(...inline(t.slice(2, -2), { ...o, bold: true }));
    else if (t.startsWith('`')) out.push(run(t.slice(1, -1), { ...o, font: 'Consolas', size: (o.size || 24) - 2 }));
    else if (t.startsWith('[^')) out.push(new FootnoteReferenceRun(Number(t.slice(2, -1))));
    else out.push(run(t.slice(1, -1), { ...o, italics: true }));
    last = m.index + t.length;
  }
  if (last < text.length) out.push(run(text.slice(last), o));
  return out;
}

const body = (text) => new Paragraph({ children: inline(text), alignment: AlignmentType.JUSTIFIED,
  spacing: { ...LINE_15, before: 0, after: 120 }, indent: { firstLine: Math.round(1.25 * CM) } });
const centered = (text, o = {}) => new Paragraph({ children: inline(text, o), alignment: AlignmentType.CENTER,
  spacing: { ...(o.single ? LINE_1 : LINE_15), before: o.before || 0, after: o.after || 0 }, keepNext: o.keepNext });
const pageBreak = () => new Paragraph({ children: [new PageBreak()] });

function heading(text, level, o = {}) {
  // Nível 1: título de capítulo (14, negrito, caixa alta); nível 2: seção (12, negrito); nível 3: subtítulo.
  const size = level === 1 ? 28 : 24;
  return new Paragraph({
    children: [run(text, { bold: true, size })],
    heading: level === 1 ? 'Heading1' : level === 2 ? 'Heading2' : 'Heading3',
    alignment: o.center ? AlignmentType.CENTER : AlignmentType.LEFT,
    spacing: { ...LINE_15, before: level === 1 ? 0 : 360, after: level === 1 ? 480 : 240 },
    keepNext: true, keepLines: true,
  });
}

// ---------------------------------------------------------------- imagens e Mermaid
function pngSize(buf) {
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
}

/** pxPerCm: para diagramas, fixa a escala do desenho (texto com ~10 pt), sem esticar diagramas estreitos. */
function imageParagraph(file, maxWidthCm = 15, maxHeightCm = 19, pxPerCm = null) {
  const data = fs.readFileSync(file);
  const { w, h } = pngSize(data);
  // 1 cm = 37.8 px em 96 dpi (unidade usada pelo docx para ImageRun)
  let width = (pxPerCm ? Math.min(maxWidthCm, w / pxPerCm) : maxWidthCm) * 37.8;
  let height = width * h / w;
  if (height > maxHeightCm * 37.8) { height = maxHeightCm * 37.8; width = height * w / h; }
  return new Paragraph({ alignment: AlignmentType.CENTER, keepNext: true, spacing: { before: 60, after: 60 },
    children: [new ImageRun({ type: 'png', data, transformation: { width: Math.round(width), height: Math.round(height) } })] });
}

function renderMermaid(code) {
  const hash = crypto.createHash('sha1').update(code).digest('hex').slice(0, 12);
  const png = path.join(cacheDir, `mmd-${hash}.png`);
  if (!fs.existsSync(png)) {
    const src = path.join(cacheDir, `mmd-${hash}.mmd`);
    fs.writeFileSync(src, code);
    const cfg = path.join(cacheDir, 'mermaid-config.json');
    // Fonte grande e pouco espaço entre caixas: impresso em ~15 cm de largura, o texto fica perto de 10 pt.
    fs.writeFileSync(cfg, JSON.stringify({ theme: 'neutral',
      themeVariables: { fontFamily: 'Arial', fontSize: '26px', xyChart: { plotColorPalette: '#3a3a3a' } },
      flowchart: { htmlLabels: true, curve: 'basis', nodeSpacing: 28, rankSpacing: 34, padding: 10, wrappingWidth: 260,
        subGraphTitleMargin: { top: 14, bottom: 14 } },
      sequence: { actorFontSize: 24, messageFontSize: 23, noteFontSize: 22, actorMargin: 40, boxMargin: 6,
        messageMargin: 30, width: 190, height: 56 } }));
    const mmdc = path.join(__dirname, 'node_modules', '.bin', process.platform === 'win32' ? 'mmdc.cmd' : 'mmdc');
    execFileSync(mmdc, ['-p', path.join(__dirname, 'pp.json'), '-c', cfg, '-i', src, '-o', png, '-s', '3', '-b', 'white',
      '-w', '1000'], { stdio: 'ignore', shell: process.platform === 'win32' });
  }
  return png;
}

// ---------------------------------------------------------------- tabelas
function parseRow(line) {
  return line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim());
}

function buildTable(rows) {
  const header = rows[0];
  const data = rows.slice(2);
  const cols = header.length;
  // Largura proporcional ao texto mais longo de cada coluna, com mínimo para não espremer.
  const lens = header.map((h, i) => Math.max(h.length, ...data.map((r) => (r[i] || '').replace(/[`*]/g, '').length)));
  const weights = lens.map((l) => Math.min(Math.max(l, 8), 70));
  const total = weights.reduce((a, b) => a + b, 0);
  const widths = weights.map((w) => Math.floor(TEXT_WIDTH * w / total));
  widths[cols - 1] += TEXT_WIDTH - widths.reduce((a, b) => a + b, 0);
  const border = { style: BorderStyle.SINGLE, size: 4, color: '7F7F7F' };
  const borders = { top: border, bottom: border, left: border, right: border };
  const cell = (text, i, isHeader, shade) => new TableCell({
    width: { size: widths[i], type: WidthType.DXA }, borders, verticalAlign: VerticalAlign.CENTER,
    margins: { top: 50, bottom: 50, left: 90, right: 90 },
    shading: isHeader ? { type: ShadingType.CLEAR, color: 'auto', fill: '000000' }
      : shade ? { type: ShadingType.CLEAR, color: 'auto', fill: 'E7E6E6' } : undefined,
    children: [new Paragraph({ spacing: { ...LINE_1, before: 0, after: 0 },
      alignment: (cols > 4 && i > 0) ? AlignmentType.CENTER : AlignmentType.LEFT,
      children: inline(text, { size: 20, bold: isHeader, color: isHeader ? 'FFFFFF' : undefined }) })],
  });
  return new Table({
    width: { size: TEXT_WIDTH, type: WidthType.DXA }, columnWidths: widths,
    rows: [new TableRow({ tableHeader: true, cantSplit: true, children: header.map((h, i) => cell(h, i, true)) }),
      ...data.map((r, k) => new TableRow({ cantSplit: true, children: header.map((_, i) => cell(r[i] || '', i, false, k % 2 === 1)) }))],
  });
}

// ---------------------------------------------------------------- leitura do Markdown
const md = fs.readFileSync(mdPath, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
const lines = md.split(/\r?\n/);
const footnotes = {};
for (const l of lines) {
  const m = l.match(/^\[\^(\d+)\]:\s*(.*)$/);
  if (m) footnotes[m[1]] = m[2];
}

const resumo = [];
const abstract = [];
const content = [];
const toc = []; // { level, text, key }
const captions = { Figura: [], Tabela: [], Quadro: [] };
let mode = 'pre';
let target = null;
let pendingCaption = null;
const pageOf = (key) => (pages[key] !== undefined ? String(pages[key]) : '00');

for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  if (/^\[\^\d+\]:/.test(line)) continue;
  if (line.startsWith('## RESUMO')) { mode = 'resumo'; continue; }
  if (line.startsWith('## ABSTRACT')) { mode = 'abstract'; continue; }
  if (mode === 'pre') continue;
  if (mode === 'resumo' || mode === 'abstract') {
    if (line.startsWith('# ')) { mode = 'body'; i--; continue; }
    if (line.trim()) (mode === 'resumo' ? resumo : abstract).push(line.trim());
    continue;
  }
  // ---- corpo
  if (line.startsWith('# ')) {
    const text = line.slice(2).trim();
    const key = `h1:${text}`;
    toc.push({ level: 1, text, key });
    if (content.length) content.push(pageBreak());
    content.push(heading(text, 1));
    continue;
  }
  if (line.startsWith('## ')) {
    const text = line.slice(3).trim();
    toc.push({ level: 2, text, key: `h2:${text}` });
    content.push(heading(text, 2));
    continue;
  }
  if (line.startsWith('### ')) { content.push(heading(line.slice(4).trim(), 3)); continue; }
  const cap = line.match(/^(Figura|Tabela|Quadro) (\d+) - (.+)$/);
  if (cap) {
    captions[cap[1]].push({ n: Number(cap[2]), title: cap[3], key: `${cap[1]} ${cap[2]}` });
    pendingCaption = cap;
    content.push(centered(`${cap[1]} ${cap[2]} - ${cap[3]}`, { single: true, before: 240, after: 120, keepNext: true }));
    continue;
  }
  if (line.startsWith('Fonte:')) {
    content.push(centered(line, { single: true, before: 120, after: 360, size: 20 }));
    pendingCaption = null;
    continue;
  }
  if (line.startsWith('```')) {
    const lang = line.slice(3).trim();
    const block = [];
    for (i++; i < lines.length && !lines[i].startsWith('```'); i++) block.push(lines[i]);
    if (lang === 'mermaid') {
      const png = renderMermaid(block.join('\n'));
      // Renderizado em escala 3 com fonte de 26 px: 220 px por cm deixam o texto com cerca de 10 pt.
      content.push(imageParagraph(png, 15, 18, 220));
    } else {
      for (const b of block) {
        content.push(new Paragraph({ spacing: { ...LINE_1, before: 0, after: 0 }, indent: { left: Math.round(1.25 * CM) },
          shading: { type: ShadingType.CLEAR, color: 'auto', fill: 'F2F2F2' },
          children: [run(b || ' ', { font: 'Consolas', size: 19 })] }));
      }
      content.push(new Paragraph({ spacing: { before: 0, after: 120 }, children: [] }));
    }
    continue;
  }
  const img = line.match(/^!\[[^\]]*\]\(([^)]+)\)$/);
  if (img) { content.push(imageParagraph(path.resolve(mdDir, img[1]), pendingCaption && /Logotipo/.test(pendingCaption[3]) ? 4 : 15.5)); continue; }
  if (line.startsWith('|')) {
    const rows = [];
    for (; i < lines.length && lines[i].startsWith('|'); i++) rows.push(parseRow(lines[i]));
    i--;
    content.push(buildTable(rows));
    continue;
  }
  if (line.startsWith('- ')) {
    content.push(new Paragraph({ numbering: { reference: 'bullets', level: 0 }, alignment: AlignmentType.JUSTIFIED,
      spacing: { ...LINE_15, before: 0, after: 60 }, children: inline(line.slice(2)) }));
    continue;
  }
  if (!line.trim()) continue;
  // Referências: alinhadas à esquerda, espaço simples, uma linha em branco entre elas.
  if (toc.length && toc[toc.length - 1].text === 'REFERÊNCIAS') {
    content.push(new Paragraph({ alignment: AlignmentType.LEFT, spacing: { ...LINE_1, before: 0, after: 240 }, children: inline(line) }));
    continue;
  }
  content.push(body(line));
}

// ---------------------------------------------------------------- elementos pré-textuais
const RIGHT_TAB = [{ type: TabStopType.RIGHT, position: TEXT_WIDTH }];
const DOT_TAB = [{ type: TabStopType.RIGHT, position: TEXT_WIDTH, leader: 'dot' }];
const titlePage = (text) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 0, after: 720 },
  children: [run(text, { bold: true, size: 28 })] });

function listPage(title, items) {
  return [titlePage(title), ...items.map((it) => new Paragraph({ tabStops: RIGHT_TAB, spacing: { ...LINE_15, before: 0, after: 0 },
    children: [run(`${it.label}  ${it.title}`), run(`\t${pageOf(it.key)}`)] })), pageBreak()];
}

function twoColPage(title, items) {
  return [titlePage(title), ...items.map(([a, b]) => new Paragraph({ tabStops: [{ type: TabStopType.LEFT, position: 2 * CM }],
    spacing: { ...LINE_15, before: 0, after: 0 }, children: [run(a), run(`\t${b}`)] })), pageBreak()];
}

const capa = [
  centered(CURSO, { bold: true }),
  ...NOMES.map((n, k) => centered(n, { bold: true, before: k === 0 ? 2600 : 0 })),
  centered(TITULO, { bold: true, before: 2200 }),
  centered(LOCAL, { bold: true, before: 4200, single: true }),
  centered(ANO, { bold: true, single: true }),
  pageBreak(),
];
const rosto = [
  ...NOMES.map((n) => centered(n, { bold: true })),
  centered(TITULO, { bold: true, before: 2000 }),
  new Paragraph({ alignment: AlignmentType.JUSTIFIED, indent: { left: 8 * CM }, spacing: { ...LINE_15, before: 1800, after: 0 },
    children: [run(NATUREZA)] }),
  new Paragraph({ alignment: AlignmentType.LEFT, indent: { left: 8 * CM }, spacing: { ...LINE_15, before: 0, after: 0 },
    children: [run(ORIENTADOR)] }),
  centered(LOCAL, { bold: true, before: 3400, single: true }),
  centered(ANO, { bold: true, single: true }),
  pageBreak(),
];
const resumoPage = (title, paras, kwLabel) => [titlePage(title),
  ...paras.filter((p) => !p.startsWith('**')).map((p) => new Paragraph({ alignment: AlignmentType.JUSTIFIED,
    spacing: { ...LINE_1, before: 0, after: 480 }, indent: { firstLine: Math.round(1.25 * CM) }, children: inline(p) })),
  ...paras.filter((p) => p.startsWith('**')).map((p) => new Paragraph({ alignment: AlignmentType.JUSTIFIED,
    spacing: { ...LINE_1, before: 240, after: 0 }, children: inline(p) })),
  pageBreak()];

const fig = captions.Figura.map((c) => ({ label: `FIGURA ${c.n}.`, title: c.title, key: c.key }));
const tab = captions.Tabela.map((c) => ({ label: `TABELA ${c.n}.`, title: c.title, key: c.key }));
const qua = captions.Quadro.map((c) => ({ label: `QUADRO ${c.n}.`, title: c.title, key: c.key }));

const sumario = [titlePage('SUMÁRIO'), ...toc.map((t) => new Paragraph({
  tabStops: DOT_TAB, indent: t.level === 2 ? { left: Math.round(0.6 * CM) } : undefined,
  spacing: { ...LINE_15, before: 0, after: 0 },
  children: [run(t.text, { bold: t.level === 1 }), run(`\t${pageOf(t.key)}`, { bold: t.level === 1 })],
}))];

const pre = [...capa, ...rosto, ...resumoPage('RESUMO', resumo), ...resumoPage('ABSTRACT', abstract),
  ...listPage('LISTA DE ILUSTRAÇÕES', fig), ...listPage('LISTA DE TABELAS', tab), ...listPage('LISTA DE QUADROS', qua),
  ...twoColPage('LISTA DE ABREVIATURAS', ABREVIATURAS), ...twoColPage('LISTA DE SIGLAS', SIGLAS), ...sumario];

// ---------------------------------------------------------------- documento
const margin = { top: 3 * CM, left: 3 * CM, bottom: 2 * CM, right: 2 * CM, header: Math.round(1.5 * CM), footer: CM };
const page = { size: { width: 11906, height: 16838 }, margin };
const fnotes = {};
for (const [n, text] of Object.entries(footnotes)) {
  fnotes[n] = { children: [new Paragraph({ alignment: AlignmentType.JUSTIFIED, spacing: LINE_1, children: inline(text, { size: 20 }) })] };
}

const doc = new Document({
  creator: 'NEXCORE', title: TITULO, description: 'Documentação do projeto NEXCORE',
  styles: {
    default: { document: { run: { font: FONT, size: 24 } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 28, bold: true }, paragraph: { spacing: { before: 0, after: 480 }, keepNext: true, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 24, bold: true }, paragraph: { spacing: { before: 360, after: 240 }, keepNext: true, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 24, bold: true }, paragraph: { spacing: { before: 360, after: 240 }, keepNext: true, outlineLevel: 2 } },
    ],
  },
  numbering: { config: [{ reference: 'bullets', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•',
    alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: Math.round(1.9 * CM), hanging: Math.round(0.6 * CM) } } } }] }] },
  footnotes: fnotes,
  sections: [
    // Pré-textuais: contam páginas, mas sem número impresso (ABNT).
    { properties: { page }, children: pre },
    // Textuais e pós-textuais: número no canto superior direito, continuando a contagem.
    { properties: { page },
      headers: { default: new Header({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
        children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 20 })] })] }) },
      footers: { default: new Footer({ children: [] }) },
      children: content },
  ],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(outPath, buf);
  const index = { titles: toc.map((t) => ({ key: t.key, text: t.text, level: t.level })),
    captions: [...captions.Figura, ...captions.Tabela, ...captions.Quadro].map((c) => ({ key: c.key, text: c.key + ' - ' + c.title })) };
  // Índice de títulos e legendas para gerar.py medir as páginas (fica no cache, fora do repositório).
  fs.writeFileSync(path.join(cacheDir, path.basename(outPath).replace(/\.docx$/, '.indice.json')), JSON.stringify(index, null, 1));
  console.log(`ok: ${outPath} (${(buf.length / 1024).toFixed(0)} KB; ${toc.length} títulos, ${fig.length} figuras, ${tab.length} tabelas, ${qua.length} quadros)`);
});
