# -*- coding: utf-8 -*-

# -- Project information -----------------------------------------------------

project = 'V-Sim'
copyright = '2018, Andres Castellanos'
author = 'Andres Castellanos'

# The short X.Y version
version = 'vsim-v1.0.0'
# The full version, including alpha/beta/rc tags
release = 'vsim-v1.0.0'


# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = [
    'sphinx.ext.coverage',
    'sphinx.ext.mathjax',
]

# Add any paths that contain templates here, relative to this directory.
templates_path = []

# The suffix(es) of source filenames.
source_suffix = '.rst'

# The master toctree document.
master_doc = 'index'

# The language for content autogenerated by Sphinx.
language = None

# Exclude patterns for files and directories.
exclude_patterns = []

# The name of the Pygments (syntax highlighting) style to use.
pygments_style = 'manni'


# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.
html_theme = 'sphinx_rtd_theme'

# Theme options.
html_theme_options = {
    'canonical_url': '',
    'logo_only': True,
    'display_version': True,
    'collapse_navigation': False
}

# Static files and dirs.
html_static_path = ['_static']

# Sidenav logo.
html_logo = '_static/img/vsim-logo.png'

# Favicon.
html_favicon = '_static/img/vsim-logo-v.png'

# Vsim Theme
html_context = {
    'css_files': [
        'https://fonts.googleapis.com/css?family=Lato',
        '_static/css/vsim_theme.css'
    ]
}

html_show_sourcelink = True


# -- Options for HTMLHelp output ---------------------------------------------

# Output file base name for HTML help builder.
htmlhelp_basename = 'V-Simdoc'


# -- Options for LaTeX output ------------------------------------------------

latex_elements = { }

# Grouping the document tree into LaTeX files.
latex_documents = [
    (master_doc, 'V-Sim.tex', 'V-Sim Documentation',
     'Andres Castellanos', 'manual'),
]


# -- Options for manual page output ------------------------------------------

# One entry per manual page.
man_pages = [
    (master_doc, 'v-sim', 'V-Sim Documentation',
     [author], 1)
]


# -- Options for Texinfo output ----------------------------------------------

# Grouping the document tree into Texinfo files.
texinfo_documents = [
    (master_doc, 'V-Sim', 'V-Sim Documentation',
     author, 'V-Sim', 'One line description of project.',
     'Miscellaneous'),
]