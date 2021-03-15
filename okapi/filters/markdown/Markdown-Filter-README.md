# Markdown Filter

## Summary

The mark down filter, filter id okf_md, is to handle Markdown files.
There is no unified Markdown specification. This filter tries to handle
most common Markdown syntax with a slight bias towards GitHub Markdown syntax.

## Features and Limitations

Current version has these features and limitations.

### HTML Attribute Text Extraction

The values assigned to the "alt" and other attributes in `<img>` and other HTML tags will be extracted for translation.
Below table lists the attributes and the tags that will be extracted by default.

| tag | attributes |
| --- | ---------- |
| a | accesskey, title |
| applet | alt |
| area | accesskey, area, alt |
| button | accesskey, value |
| img | alt, title |
| input | accesskey, alt, placeholder, title, value \* |
| isindex | prompt |
| label | accesskey |
| legend | accesskey |
| object | standby |
| option | label, value |
| optgroup | label |
| param | value |
| table | summary |
| td | abbr |
| textarea | accesskey |


\*: Extraction may not be supported depending on the type value.

### MathML Support

The `<math>` HTML tag and other tags defined in [Mathematical Markup Language](https://www.w3.org/TR/MathML), also known as MathML, can be used in HTML blocks, by default.

Anything written within a math element is considered untranslatable and will not be extracted to the XLIF file.
Like the table tags, text following `</math>` on the same line is treated as a part of the HTML block. This means the whitespace after the end tag is ignored and `*text*` is displayed as it is.

### HTML Subfilter Configuration Override

The Markdown filter uses the HTML subfilter to parse the HTML elements. The configuration of this filter can be overriden by specifying the Markdown configuration parameter htmlSubfilter.

The default configuration file can be found in examples/okf_markdown@for_markdown.fprm, and can be used as the base for modification. Note changing the configuration of the HTML subfilter may cause unintended behavior. Please use this feature at your own risk.

The HTML subfilter is applied to each HTML piece in the Markdown file. Because of how the Markdown works, not all the text that you might think is part of an HTML portion is not treated so. For example, in general a blank line breaks the flaw of the text and what seems to be an HTML part of the text is not treated as such.  For example:

```
<div>
  This is part of the *HTML* text but....

  This is not. This is treated as a new *Markdown* paragraph.

</div>
```

In above, `*HTML*` will be displayed as such but `*Markdown*` will be displayed without astrisks and in bold or italic.
Therefore, the asterisks of `*HTML*` will be part of the translatable text while the asterisks of `*Markdown*` will be treated as codes.


### Whitespace Handling Issues
Within an HTML block, white spaces may be removed. Note this does not
change the meaning of the HTML block because whitespaces
are not usually kept except within `<pre>` and `</pre>` per the HTML
standard specification.


