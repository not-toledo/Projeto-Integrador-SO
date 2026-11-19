"""Gera docs/NEXCORE-documentacao.docx em duas passagens e confere as páginas.

1. Gera o .docx com os números de página das listas e do sumário em "00".
2. Converte para PDF (LibreOffice) e encontra a página de cada título e legenda.
3. Gera o .docx de novo com os números reais e confere que nada mudou de página.
4. Salva as páginas em PNG (para revisão visual) em .cache/paginas/.

Uso (na pasta docs/doc-build, depois de "npm install"):
    python gerar.py
Requer: Node.js, LibreOffice (soffice) e os pacotes Python pypdf e pypdfium2.
"""
import json
import os
import re
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DOCS = os.path.dirname(HERE)
MD = os.path.join(DOCS, 'NEXCORE-documentacao.md')
OUT = os.path.join(DOCS, 'NEXCORE-documentacao.docx')
CACHE = os.path.join(HERE, '.cache')
TMP_DOCX = os.path.join(CACHE, 'passagem.docx')
PAGES = os.path.join(CACHE, 'paginas.json')
SOFFICE = shutil.which('soffice') or r'C:\Program Files\LibreOffice\program\soffice.exe'

import pypdf  # noqa: E402
import pypdfium2  # noqa: E402


def build(docx, pages=None):
    args = ['node', os.path.join(HERE, 'gerar-docx.js'), MD, docx] + ([pages] if pages else [])
    subprocess.run(args, check=True, cwd=HERE)


def to_pdf(docx):
    subprocess.run([SOFFICE, '--headless', '--convert-to', 'pdf', '--outdir', CACHE, docx], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return os.path.splitext(os.path.join(CACHE, os.path.basename(docx)))[0] + '.pdf'


def compact(text):
    return re.sub(r'\s+', '', text)


def measure(pdf, index):
    texts = [page.extract_text() or '' for page in pypdf.PdfReader(pdf).pages]
    # O corpo começa depois do sumário (última página com linhas pontilhadas).
    start = max(i for i, t in enumerate(texts) if '.....' in t) + 1
    bodies = [compact(t) for t in texts]
    found = {}
    for item in index['titles'] + index['captions']:
        needle = compact(item['text'])
        page = next((i for i in range(start, len(texts)) if needle in bodies[i]), None)
        if page is None:
            sys.exit(f'Não encontrado no PDF: {item["text"]}')
        found[item['key']] = page + 1  # página física, contando desde a capa
    return found, len(texts)


def main():
    os.makedirs(CACHE, exist_ok=True)
    build(TMP_DOCX)
    index = json.load(open(os.path.join(CACHE, 'passagem.indice.json'), encoding='utf-8'))
    first, total = measure(to_pdf(TMP_DOCX), index)
    json.dump(first, open(PAGES, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)

    build(OUT, PAGES)
    pdf = to_pdf(OUT)
    second, total2 = measure(pdf, index)
    if second != first or total2 != total:
        diff = {k: (first.get(k), second.get(k)) for k in second if first.get(k) != second.get(k)}
        sys.exit(f'As páginas mudaram entre as passagens: {diff}')

    out_dir = os.path.join(CACHE, 'paginas')
    shutil.rmtree(out_dir, ignore_errors=True)
    os.makedirs(out_dir)
    document = pypdfium2.PdfDocument(pdf)
    for i in range(len(document)):
        document[i].render(scale=1.2).to_pil().save(os.path.join(out_dir, f'pagina-{i + 1:02d}.png'))
    print(f'ok: {OUT} ({total} páginas); PDF de conferência: {pdf}; imagens em {out_dir}')


if __name__ == '__main__':
    main()
