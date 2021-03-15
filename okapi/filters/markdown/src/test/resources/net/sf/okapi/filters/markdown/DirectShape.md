<div class="tablenoborder">

<table cellpadding="4" cellspacing="0" summary="" id="GUID-DF7B9D4A-5A8A-4E39-8721-B7782CBD7730__TABLE_E9824E5167E64B28B657C8145321BEE4" class="table" frame="border" border="1" rules="all">

<tbody class="tbody">

<tr class="row">

<td class="entry" valign="top">

**Code Region: Create a DirectShape**

</td>

</tr>

<tr class="row">

<td class="entry" valign="top">

<pre class="pre codeblock prettyprint">// Create a DirectShape Sphere
public void CreateSphereDirectShape(Document doc)
{
    List<Curve> profile = new List<Curve>();

    // first create sphere with 2' radius
    XYZ center = XYZ.Zero;
    double radius = 2.0;    
    XYZ profile00 = center;
    XYZ profilePlus = center + new XYZ(0, radius, 0);
    XYZ profileMinus = center - new XYZ(0, radius, 0);

    profile.Add(Line.CreateBound(profilePlus, profileMinus));
    profile.Add(Arc.Create(profileMinus, profilePlus, center + new XYZ(radius, 0, 0)));

    CurveLoop curveLoop = CurveLoop.Create(profile);
    SolidOptions options = new SolidOptions(ElementId.InvalidElementId, ElementId.InvalidElementId);

    Frame frame = new Frame(center, XYZ.BasisX, -XYZ.BasisZ, XYZ.BasisY);
    if (Frame.CanDefineRevitGeometry(frame) == true)
    {
        Solid sphere = GeometryCreationUtilities.CreateRevolvedGeometry(frame, new CurveLoop[] { curveLoop }, 0, 2 * Math.PI, options);
        using (Transaction t = new Transaction(doc, "Create sphere direct shape"))
        {
            t.Start();
            // create direct shape and assign the sphere shape
            DirectShape ds = DirectShape.CreateElement(doc, new ElementId(BuiltInCategory.OST_GenericModel));

            ds.ApplicationId = "Application id";
            ds.ApplicationDataId = "Geometry object id";
            ds.SetShape(new GeometryObject[] { sphere });
            t.Commit();
        }
    }
}
</pre>

</td>

</tr>

</tbody>

</table>
