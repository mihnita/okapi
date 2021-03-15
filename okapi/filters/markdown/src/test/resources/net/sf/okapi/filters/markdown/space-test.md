Paragraphs with varying number of leading spaces:

 One space after a newline before me will be rendered without a leading space.

  Two spaces after a newline before me will be rendered without a leading space.

   Three spaces after a newline before me will be rendered without a leading space.

    Four spaces after a newline before me will be rendered with no leading space in code block.

     Five spaces after a newline before me will be rendered with one leading space in code block.

      Six spaces after a newline before me will be rendered with two leading spaces in code block.


> One space after `>`
>  two spaces after `>`
>   three spaces after `>`
>    four spaces after `>`
>     fieve spaces after `>`
> are all concatenated and rendered as a paragraph.


A numbered list with leading spaces of more than two:

1.  PointCloudType.GetPath() - The path of the link source from which the points are loaded
1.  Application.PointCloudsRootPath - The root path for point cloud files which is used by Revit to calculate relative paths to point cloud files


A bullet list with leading spaces of more than two, and inline markups:

*   **PointCloudType** - type of point cloud loaded into a Revit document. Each PointCloudType maps to a single file or identifier (depending upon the type of Point Cloud Engine which governs it).
*   **PointCloudInstance** - an instance of a point cloud in a location in the Revit project.

