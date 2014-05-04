from os import getcwd
from os.path import join as j
from shutil import copyfile
import re

from apikeys import GOOGLE_APIKEY

BASEDIR = getcwd()
TEMPLATE_BASE = j(BASEDIR, 'kenenkaupunki-design')
OUTPUT_DIR = j(BASEDIR, 'resources/public')

PARTIAL_RE = re.compile(r'<%= partial "([^/]+)/([^"]+)" %>')
YIELD_RE = re.compile(r'<%= yield %>')
HEAD_CLOSE_RE = re.compile(r'</head>')
BODY_CLOSE_RE = re.compile(r'</body>')
REMOVE_RE = re.compile(r'<%= (stylesheet|javascript|title )[^%]+%>')

def inline_templates(layout):
    partial_contents = "\n"
    for match in PARTIAL_RE.finditer(layout):
        match_id = match.group(2)
        partial_filename = "{0}/_{1}.erb".format(match.group(1), match_id)
        partial_contents += '<script id="{0}" type="text/template">'.format(
            match_id
        )
        with open(j(TEMPLATE_BASE, 'source', partial_filename), 'r') as f:
            partial_contents += f.read()
        partial_contents += "</script>\n\n"
    return partial_contents

def placeholder(id):
    return '<div data-placeholder="true" data-template="{0}"></div>'.format(id)

def add_sublayout(layout):
    partial_contents = "\n"
    with open(j(TEMPLATE_BASE, 'source', 'kaupunginosa.html.erb')) as f:
        neighborhood_layout = f.read()
        partial_contents += '<script id="kaupunginosa" type="text/template">'
        partial_contents += YIELD_RE.sub('', neighborhood_layout)
        partial_contents += '</script>'
        partial_contents += inline_templates(neighborhood_layout)
    return PARTIAL_RE.sub(placeholder('\g<2>'), partial_contents)

def transform_layout(layout):
    partial_contents = inline_templates(layout)
    partial_contents += '<link href="css/screen.css" rel="stylesheet" type="text/css">'
    partial_contents += add_sublayout(layout)
    return (
        REMOVE_RE.sub(
            '',
            HEAD_CLOSE_RE.sub(
                partial_contents + '\g<0>',
                PARTIAL_RE.sub(
                    placeholder('\g<2>'), YIELD_RE.sub(
                        placeholder("kaupunginosa"), BODY_CLOSE_RE.sub(
                            '<script type="text/javascript" '
                            ' src="js/underscore-min.js"></script>' +
                            '<script type="text/javascript"' +
                            ' src="https://maps.googleapis.com/maps/api/js?key={0}&sensor=false"> '.format(GOOGLE_APIKEY) + 
                            '</script>' + 

                            '<script src="js/town.js"></script>' +
                            '\g<0>', layout))))))

    


with open(j(TEMPLATE_BASE, 'source/layouts/layout.erb'), 'r') as f:
    base_layout = f.read()

with open(j(OUTPUT_DIR, 'index.html'), 'w') as f:
    f.write(
        transform_layout(
            base_layout))

copyfile(j(TEMPLATE_BASE, 'build/stylesheets', 'screen.css'),
         j(OUTPUT_DIR, 'css', 'screen.css'))

