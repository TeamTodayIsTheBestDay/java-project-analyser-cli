// The project function defines how your document looks.
// It takes your content and some metadata and formats it.
// Go ahead and customize it to your liking!
#let project(title: "", authors: (), body) = {
  // Set the document's basic properties.
  set document(author: authors.map(a => a.name), title: title)
  set page(numbering: "1", number-align: center)
  set text(font: ("Linux Libertine", "Noto Serif CJK SC"), lang: "zh")

  set text(size: 1.1em)

  // Title page.
  // The page can contain a logo if you pass one with `logo: "logo.png"`.
  v(0.6fr)
  v(8fr)

  text(2em, weight: 700, title)
  
  v(0.6fr)

  text(1.6em, weight: 500, [Course Project for Java EE, Tongji University])

  // Author information.
  pad(
    top: 0.7em,
    right: 20%,
    grid(
      columns: (1fr,) * calc.min(3, authors.len()),
      gutter: 1em,
      ..authors.map(author => align(start)[
        *#author.name* \
        #author.email \
        \
        Tongji University
      ]),
    ),
  )

  v(2.4fr)
  pagebreak()


  // Main body.
  set par(justify: true)

  set par(leading: 0.8em)

   // Set paragraph spacing.
  show par: set block(above: 1em, below: 1em)

  show heading.where(level: 3): it => text(size: 1.1em, it)

  show raw.where(block: true): it => {
    set par(justify: false)
    block(
      fill: luma(230),
      inset: 8pt,
      radius: 4pt,
      width: 100%,
      text(font: ("Noto Sans Mono","Noto Sans CJK SC"), it)
    )
    set par(justify: true)
  }

  show raw.where(block: false): it => {
    text(font: ("Noto Sans Mono","Noto Sans CJK SC"), it)
  }

  body
}

#let pd() = {
  v(1em)
}

#let important(content) = {
  block(
    fill: rgb("#FFFFCC"),
    inset: 12pt,
    radius: 4pt,
    width: 100%,
    [
      *Important*

      #text(content)
    ]
  )
}