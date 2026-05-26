#!/usr/bin/env python3
import zipfile, xml.etree.ElementTree as ET, os, json, sys

def extract(path):
    if not os.path.exists(path):
        return {'error':'file not found', 'path': path}
    try:
        with zipfile.ZipFile(path) as z:
            data = z.read('word/document.xml')
    except Exception as e:
        return {'error':'cannot read docx', 'exception': str(e)}
    root = ET.fromstring(data)
    ns = {'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    headings = []
    for p in root.findall('.//w:p', ns):
        pPr = p.find('w:pPr', ns)
        level = None
        if pPr is not None:
            pStyle = pPr.find('w:pStyle', ns)
            if pStyle is not None:
                val = pStyle.get('{http://schemas.openxmlformats.org/wordprocessingml/2006/main}val')
                if val:
                    import re
                    m = re.search(r'([Hh]eading)[\s_]?([0-9]+)', val)
                    if m:
                        level = int(m.group(2))
                    else:
                        m2 = re.search(r'(\d+)$', val)
                        if m2:
                            level = int(m2.group(1))
        texts = [t.text for t in p.findall('.//w:t', ns) if t.text]
        text = ''.join(texts).strip()
        if level is not None and text:
            headings.append({'level': level, 'text': text})
    return {'headings': headings}

if __name__ == '__main__':
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
    path = os.path.join(root, 'SablonLicenta2026.docx')
    if len(sys.argv) > 1:
        path = sys.argv[1]
    out = extract(path)
    print(json.dumps(out, ensure_ascii=False, indent=2))
