# A simple ordered list

<ol>
 <li>Ordered list item 1</li>
  <li>Ordered list item 2</li> <!-- indentation off -->
</ol>

# A simple unordered list

<ul>
 <li>Unordered list item 1</li>
 <li>Unordered list item 2</li>
</ul>


# A math block

<math xmlns="http://www.w3.org/1998/Math/MathML">
  <mstyle displaystyle="true">
    <mover>
      <mrow>
        <mi> H </mi>
      </mrow>
      <mrow>
        <mo> &#x2192;<!--rightwards arrow--> </mo>
      </mrow>
    </mover>
  </mstyle></math>


# Math blocks in an unordered list

<ul>
<li><math xmlns="http://www.w3.org/1998/Math/MathML">
  <mstyle displaystyle="true">
    <mover>
      <mrow>
        <mi> H </mi>
      </mrow>
      <mrow>
        <mo> &#x2192;<!--rightwards arrow--> </mo>
      </mrow>
    </mover>
  </mstyle></math>is the magnetic field intensity</li>
<li><math xmlns="http://www.w3.org/1998/Math/MathML">
  <mstyle displaystyle="true">
    <mover>
      <mrow>
        <mi> J </mi>
      </mrow>
      <mrow>
        <mo> &#x2192;<!--rightwards arrow--> </mo>
      </mrow>
    </mover>
  </mstyle></math>is the conduction current density</li>
</ul>


# A math block in single line (unsupported)

<math xmlns="http://www.w3.org/1998/Math/MathML"><mstyle displaystyle="true"><mover><mrow><mi> H </mi></mrow><mrow><mo> &#x2192;<!--rightwards arrow--> </mo></mrow></mover></mstyle></math>


# Alt attribute in IMG tag

## With valid image URL

<img src="https://www.spartansoftwareinc.com/wp-content/uploads/2016/07/Logo-Text.png" alt="Spartan logo"> is here to help you.

## WIth invalid image URL

<img src="https://www.devnull.com/spartan-logo.jpg" alt="Spartan Software"> is here to help you.


# Title attribute in A tag

<a href="https://www.spartansoftwareinc.com" title="Spartan logo" alt="Spartan logo image unavailable" />Spartan Software Inc.</a> is here to help you!

