 <% 
	Dim strPage
	strPage = Request.Servervariables("URL")
	strPage = Right(strPage, (Len(strPage) - InStrRev(strPage, "/")))
%>
        <div id="nav_side" class="accordion">
          <ul>
                <li><a href="link1.asp" <% If strPage = "link2.asp" Then %> class="current" <%End If%> >Pay My Visa Fee</a></li>
          </ul>

        </div>