
import java.net.MalformedURLException;
import java.net.URL;
import java.io.*;
import java.util.*;
import javax.xml.xpath.*;


import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


class ErrorHandler extends DefaultHandler {

	Boolean hasWarnings = false;
	Boolean hasErrors = false;
	Boolean hasFatalErrors = false;
	String displayMessage = "";

	public ErrorHandler(){};

	public boolean hasWarnings(){
		return hasWarnings;
	}

	public boolean hasErrors(){
		return hasErrors;
	}

	public boolean hasFatalErrors(){
		return hasFatalErrors;
	}

	public String getDisplayMessage(){
		return displayMessage;
	}

	public void warning(SAXParseException spe) {
		this.hasWarnings=true;
		this.displayMessage=spe.toString();
	}

	public void error(SAXParseException spe) {
		this.hasErrors=true;
		this.displayMessage=spe.toString();
	}

	public void fatalError(SAXParseException spe) {
		this.hasFatalErrors=true;
		this.displayMessage=spe.toString();
	}

}


public class Sint35P2 extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String RAIZ="http://gssi.det.uvigo.es/users/agil/public_html/SINT/17-18/mml2001.xml";
	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	private static String xsdPath;
	private TreeMap<String,Document> movies = new TreeMap<String,Document>();
	private ArrayList<String> MMLS = new ArrayList<String>();
	private ArrayList<String> moviesErroneas = new ArrayList<String>();
	private TreeMap<String,String> warningMap = new TreeMap<String,String>();
	private TreeMap<String,String> errorMap = new TreeMap<String,String>();
	private TreeMap<String,String> fatalErrorMap = new TreeMap<String,String>();
	private TreeMap<String,String> lenguajes = new TreeMap<String,String>(Collections.reverseOrder());
	private TreeMap<String,String> actoresOscar = new TreeMap<String,String>();
	private TreeMap<String,String> actoresSinOscar = new TreeMap<String,String>();
	private TreeMap<String,String> paisesIdiomas = new TreeMap<String,String>();
	private static TreeMap<String,Integer> peliculasPaisNumero = new TreeMap<String,Integer>();
	private TreeMap<String,String> pelisIPfinal = new TreeMap<String,String>();
	ArrayList<String> listaPaisesaArrayList = new ArrayList<String>();
	String pais_examen;
	String color_examen;
	String idioma_examen;
	String color_examen_traducido;


	public void examen(){
		Document doc=null;
		try{
			DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
			DocumentBuilder db=dbf.newDocumentBuilder();
			doc=db.parse("http://gssi.det.uvigo.es/users/agil/public_html/ex1.xml");

		}
		catch(Exception e){
			e.printStackTrace();

		}
		try{
			XPathFactory xpathFactory=XPathFactory.newInstance();
			XPath xpath=xpathFactory.newXPath();
			pais_examen=(String) xpath.evaluate("/examen/pais",doc.getDocumentElement(),XPathConstants.STRING);
			idioma_examen=(String) xpath.evaluate("/examen/text()[normalize-space()]",doc.getDocumentElement(),XPathConstants.STRING);
			color_examen=(String) xpath.evaluate("/examen/pais/@color",doc.getDocumentElement(),XPathConstants.STRING);

		}
		catch (Exception e){

		}

	}


	public void init(ServletConfig conf) throws ServletException {
		super.init(conf);
		ServletContext context = conf.getServletContext();
		String realPath = context.getRealPath("/");
		xsdPath = realPath + "mml.xsd";
		try {
			processXML(RAIZ);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Boolean isUrl (String urlOrFilename) {
		if (urlOrFilename.startsWith("http://")){
			return true;
		} else return false;
	}

	public void processXML(String urlOrFilename) throws MalformedURLException, IOException{
		ErrorHandler eh = new ErrorHandler();
		Document doc = null;
		DocumentBuilder db = null;
		DocumentBuilderFactory dbf = null;
		NodeList nl = null;
		String finalUrl = null;

		if(!isUrl(urlOrFilename)){
			finalUrl = "http://gssi.det.uvigo.es/users/agil/public_html/SINT/17-18/" + urlOrFilename;
		} else{
			finalUrl = urlOrFilename;
		}

		File schemaSource = new File(xsdPath);
		InputStream urlStream = new URL(finalUrl).openStream();

		dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(true);
		dbf.setValidating(true);
		try {
			dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
		} catch (IllegalArgumentException e){
			e.printStackTrace();
		}

		if (schemaSource != null) {
			dbf.setAttribute(JAXP_SCHEMA_SOURCE, schemaSource);
		}

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		db.setErrorHandler(eh);

		try {
			doc = db.parse(urlStream,finalUrl);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (moviesErroneas.contains(finalUrl)){
			return;
		}

		if(checkAndRegisterIssues(eh,finalUrl) || doc == null){
			moviesErroneas.add(finalUrl);
		} else{
			movies.put(doc.getDocumentElement().getElementsByTagName("Anio").item(0).getTextContent(), doc);
			nl = doc.getDocumentElement().getElementsByTagName("MML");
			for(int i= 0; i<nl.getLength(); i++){
				if(!MMLS.contains(nl.item(i).getTextContent())){
					MMLS.add(nl.item(i).getTextContent());
					processXML(nl.item(i).getTextContent());
				}
			}
		}

	}

	public void fase02(PrintWriter out, Boolean modoAuto,String pass){

		if(modoAuto){

			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.println("<errores>");
			out.println("  <warnings>");

			for (Map.Entry<String, String> entry : warningMap.entrySet()) {
				out.println("    <warning>");
				out.println("      <file>" + entry.getKey() + "</file>");
				out.println("      <cause>" + entry.getValue() + "</cause>");
				out.println("    </warning>");
			}
			out.println("  </warnings>");
			out.println("  <errors>");
			for (Map.Entry<String, String> entry : errorMap.entrySet()) {
				out.println("    <error>");
				out.println("      <file>" + entry.getKey() + "</file>");
				out.println("      <cause>" + entry.getValue() + "</cause>");
				out.println("    </error>");
			}
			out.println("  </errors>");
			out.println("  <fatalerrors>");
			for (Map.Entry<String, String> entry : fatalErrorMap.entrySet()) {
				out.println("    <fatalerror>");
				out.println("      <file>" + entry.getKey() + "</file>");
				out.println("      <cause>" + entry.getValue() + "</cause>");
				out.println("    </fatalerror>");
			}
			out.println("  </fatalerrors>");
			out.println("</errores>");


		} else{

			out.println("<!DOCTYPE html>");
			out.println("<html>");
			out.println("<head>");
			out.println("<title>Servicio de consulta de películas</title>");
			out.println("<link rel='stylesheet' href='mml.css'>");
			out.println("</head>");
			out.println("<body>");
			out.println("<div class='page-wrap'>");
			out.println("<h1>Servicio de consulta de películas</h1>");
			out.println("<h3>Se han encontrado " + String.valueOf(warningMap.size()) + " ficheros con warnings:</h3>");
			out.println("<ul>");
			for (Map.Entry<String, String> entry : warningMap.entrySet()) {
				out.println("<li>" + entry.getKey() + "---" + entry.getValue() + "</li>");
			}
			out.println("</ul>");
			out.println("<h3>Se han encontrado " + String.valueOf(errorMap.size()) + " ficheros con errores:</h3>");
			out.println("<ul>");
			for (Map.Entry<String, String> entry : errorMap.entrySet()) {
				out.println("<li>" + entry.getKey() + "---" + entry.getValue() + "</li>");
			}
			out.println("</ul>");
			out.println("<h3>Se han encontrado " + String.valueOf(fatalErrorMap.size()) + " ficheros con errores fatales:</h3>");
			out.println("<ul>");
			for (Map.Entry<String, String> entry : fatalErrorMap.entrySet()) {
				out.println("<li>" + entry.getKey() + "---" + entry.getValue() + "</li>");
			}
			out.println("</ul>");
			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='hidden' name='pfase' value='01' />");
			out.println("<input type=submit value='Atrás'>");
			out.println("</form>");
			out.println("</div>");
			out.println("<footer class='site-footer'>");
			out.println("<p>Jonatan Gomez Fernandez</p>");
			out.println("</footer>");
			out.println("</body>");
			out.println("</html>");

		}

		return;

	}

	public Boolean checkAndRegisterIssues (ErrorHandler eh, String finalUrl){

		Boolean hasIssue = false;

		if(eh.hasWarnings()){
			hasIssue = true;
			if(warningMap.containsKey(finalUrl)){
				if(!warningMap.get(finalUrl).contains(eh.getDisplayMessage())){
					String newValue = warningMap.get(finalUrl) + " || " + eh.getDisplayMessage();
					warningMap.put(finalUrl, newValue);
				}
			} else{
				warningMap.put(finalUrl, eh.getDisplayMessage());
			}
		} if(eh.hasErrors()){
			hasIssue = true;
			if(errorMap.containsKey(finalUrl)){
				if(!errorMap.get(finalUrl).contains(eh.getDisplayMessage())){
					String newValue = errorMap.get(finalUrl) + " || " + eh.getDisplayMessage();
					errorMap.put(finalUrl, newValue);
				}
			} else{
				errorMap.put(finalUrl, eh.getDisplayMessage());
			}
		} if(eh.hasFatalErrors()){
			hasIssue = true;
			if(fatalErrorMap.containsKey(finalUrl)){
				if(!fatalErrorMap.get(finalUrl).contains(eh.getDisplayMessage())){
					String newValue = fatalErrorMap.get(finalUrl) + " || " + eh.getDisplayMessage();
					fatalErrorMap.put(finalUrl, newValue);
				}
			} else{
				fatalErrorMap.put(finalUrl, eh.getDisplayMessage());
			}
		}

		return hasIssue;

	}



	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

		examen();

		req.setCharacterEncoding("UTF-8");
		res.setCharacterEncoding("UTF-8");


		PrintWriter out = res.getWriter();
		boolean auto;
		String modo;
		String p;
		String passok = "wins1ekei7";
		String pass = req.getParameter("p");
		String pfase=req.getParameter("pfase");
		String errores="";
		String plang=null;
		String pact=null;
		String ppais=null;

		if (req.getParameter("auto") == null) {
			auto = false;
			modo = "";
		} else {
			modo = req.getParameter("auto");

		}

		if (modo.equals("si")) {
			auto = true;
			res.setContentType("text/xml");
		} else {
			auto = false;
			res.setContentType("text/html");
		}

		if (pass == null) {
			if (auto) {
				res.setContentType("text/xml");
				out.println("<?xml version='1.0' encoding='utf-8' ?>");
				out.println("<wrongRequest>no passwd</wrongRequest>");
				return;

			} else {

				res.setContentType("text/html");
				out.println("Introduzca una contraseña");
				return;
			}

		}

		if (!pass.equals(passok)) {
			if (auto) {

				res.setContentType("text/xml");
				out.println("<?xml version='1.0' encoding='utf-8' ?>");
				out.println("<wrongRequest> bad psswd</wrongRequest>");
				return;
			}

			else {
				res.setContentType("text/html");
				out.println("Contraseña incorrecta");
				return;

			}
		}

		if (pfase ==null){

			pfase="01";
		}



		switch (pfase) {

			case "01":

			fase01(out, auto, pass);
			break;

			case "02":

			fase02(out, auto, pass);
			break;

			case "21":

			fase21(out, auto, pass);
			break;

			case "22":


			plang=req.getParameter("plang");
			actoresOscar.clear();
			actoresSinOscar.clear();

			fase22(out,auto,pass,plang);
			break;

			case "23":

			peliculasPaisNumero.clear();
			listaPaisesaArrayList.clear();
			plang=req.getParameter("plang");
			pact=req.getParameter("pact");
			fase23(out,auto,pass,plang,pact);
			break;

			case "24":

			pelisIPfinal.clear();
			plang=req.getParameter("plang");
			pact=req.getParameter("pact");
			ppais=req.getParameter("ppais");
			fase24(out, auto, pass,plang,pact,ppais);
			break;


			case "25":
			plang=req.getParameter("plang");
			pact=req.getParameter("pact");
			ppais=req.getParameter("ppais");
			fase25(out, auto, pass,plang,pact,ppais);
			break;


			default:


			fase01(out, auto, pass);
			break;
		}

		return;

	}


	public void fase01(PrintWriter out, boolean auto, String pass) {



		if (auto) {
			out.println("<?xml version='1.0' encoding='utf-8' ?>");


			out.println("<service>");
			out.println("<status>OK</status>");
			out.println("</service>");


		} else {


			out.println("<html>");

			out.println("<head>");
			out.println("<title>Servicio de consulta de películas</title>");
			out.println("<link rel='stylesheet' type='text/css' href='mml.css'/>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h1>Servicio de consulta de películas</h1>");
			out.println("<h1>Bienvenido a este servicio</h1>");
			out.println("<a href='?p="+pass+"&pfase=02 '>Pulsa aquí para ver los ficheros erróneos</a>");
			out.println("<h2>Selecciona una consulta:</h2>");
			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='p'value='" + pass + "'/>");
			out.println("<input type='hidden' name='pfase' value='21' />");
			out.println("<input type='radio' checked> Películas de un actor/actriz,en un idioma,producidas en un país<br><br>");
			out.println("<input type='submit' value='Enviar'/>");
			out.println("</form>");
			out.println("<footer class='pie-pag'>");
			out.println("<p> Jonatan Gómez Fernández </p>");
			out.println("</footer>");

			out.println("</body>");
			out.println("</html>");
		}
		return;
	}



	public void fase21(PrintWriter out, boolean auto, String pass) {

		getC2Langs();

		if (auto) {

			out.println("<?xml version='1.0' encoding='utf-8' ?>");
			out.println("<langs>");
			for (Map.Entry<String, String> entry : lenguajes.entrySet()) {

				out.println("  <lang>"+entry.getKey()+"</lang>");

			}
			out.println("</langs>");

		} else {

			out.println("<html>");
			out.println("<head>");
			out.println("<link rel='stylesheet' type='text/css' href='mml.css'/>");
			out.println("</head>");

			out.println("<body>");
			out.println("<h1>Servicio de consulta de películas</h1>");
			out.println("<h2>Selecciona un idioma:</h2>");
			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='hidden' name='pfase' value='22' />");

			int numeracion=1;
			for (Map.Entry<String, String> entry : lenguajes.entrySet()) {

				out.println("<input type='radio' name='plang' value='"+entry.getKey()+"' checked/>"+numeracion+ ".- " + entry.getKey()+"<br>");
				numeracion++;
			}

			out.println("<input type='submit' value='Enviar'/>");

			out.println("</form>");
			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='pfase' value='01'/>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='submit' value='Atrás'/>");
			out.println("</form>");

			out.println("<footer class='pie-pag'>");
			out.println("<p> Jonatan Gómez Fernández </p>");
			out.println("</footer>");
			out.println("</body>");
			out.println("</html>");
		}
		return;
	}


	public void getC2Langs(){
		System.out.println("---------------------------------------------------------------------------------------------");
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		NodeList ListaPaises= null;
		NodeList ListaPelis=null;

		try{

			for(String i : movies.keySet()){
				ListaPaises  = (NodeList) xPath.evaluate("/Movies/Pais", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
				//Crea una lista de los paises
				for (int n=0;n<ListaPaises.getLength();n++){
					//Recorremos la lista de paises y pillamos el pais y lang de cada uno
					String pais= (String) xPath.evaluate("@pais", ListaPaises.item(n), XPathConstants.STRING);
					String lang= (String) xPath.evaluate("@lang", ListaPaises.item(n), XPathConstants.STRING);
					ListaPelis  = (NodeList) xPath.evaluate("/Movies/Pais[@pais='"+pais+"']/Pelicula", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
					//Hacemos una lista de las pelis de ese pais en concreto
					for (int j=0;j<ListaPelis.getLength();j++){ //Recorremos las pelis de ese pais en concreto
						String langs= (String) xPath.evaluate("@langs", ListaPelis.item(j), XPathConstants.STRING);
						if (langs.length() < 1){
							String lang_peli=lang.trim();
							lenguajes.put(lang_peli,"0");
						}
						else{

							String langu[]=langs.split(" ");
							for (int m=0;m<langu.length;m++){

								lenguajes.put(langu[m],"0");

							}
						}
					}
				}

			}
		}
		catch (Exception e)
		{

		}

		return;

	}


	public void fase22(PrintWriter out, boolean auto, String pass, String plang) {

		getC2Acts(plang);

		if (auto) {

			out.println("<?xml version='1.0' encoding='utf-8' ?>");

			if (plang ==null){

				out.println("<wrongRequest>no param:plang</wrongRequest>");
				return;

			}


			out.println("<acts>");
			for (Map.Entry<String, String> entry : actoresOscar.entrySet()) {
				String keypartida[]=entry.getKey().split("\\(");

				out.println("<ac ciudad='"+"("+keypartida[1]+"' oscar='true'>"+keypartida[0]+"</ac>");
			}

			for (Map.Entry<String, String> entry : actoresSinOscar.entrySet()) {
				String keypartida[]=entry.getKey().split("\\(");

				out.println("<ac ciudad='"+"("+keypartida[1]+"' oscar='false'>"+keypartida[0]+"</ac>");
			}
			out.println("</acts>");

		}

		else {

			if (plang ==null){
				out.println("Falta el parámetro plang");
				return;
			}

			out.println("<html>");
			out.println("<head>");
			out.println("<link rel='stylesheet' type='text/css' href='mml.css'/>");
			out.println("</head>");
			out.println("<body>");

			out.println("<h1>Servicio de consulta de películas</h1>");
			out.println("<h2>Idioma="+plang+"</h2>");
			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='hidden' name='plang' value='" + plang + "'/>");
			out.println("<input type='hidden' name='pfase' value='23' />");


			int numeracion=1;
			for (Map.Entry<String, String> entry : actoresOscar.entrySet()) {

				String keypartida[]=entry.getKey().split("\\(");
				if (actoresSinOscar.isEmpty()){
					out.println("<input type='radio' name='pact' value='"+keypartida[0]+"'checked/>"+numeracion+ ".- " + entry.getKey()+" "+entry.getValue()+"<br>");
					numeracion++;
				}
				else{
					out.println("<input type='radio' name='pact' value='"+keypartida[0]+"'/>"+numeracion+ ".- " + entry.getKey()+" "+entry.getValue()+"<br>");
					numeracion++;
				}



			}

			for (Map.Entry<String, String> entry : actoresSinOscar.entrySet()) {
				String keypartida[]=entry.getKey().split("\\(");

				out.println("<input type='radio' name='pact' value='"+keypartida[0]+"'checked/>"+numeracion+ ".- " + entry.getKey()+" "+entry.getValue()+"<br>");
				numeracion++;
			}

			out.println("<input type='submit' value='Enviar'/>");
			out.println("</form>");

			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='pfase' value='21' />");
			out.println("<input type='hidden' name='p' value='"+ pass +"'/>");
			out.println("<input type='hidden' name='plang' value='"+ plang +"'/>");
			out.println("<input type='submit' value='Atrás'/>");
			out.println("</form>");

			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='pfase' value='01'/>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");

			out.println("<input type='submit' value='Inicio'/>");
			out.println("</form>");


			out.println("<footer class='pie-pag'>");
			out.println("<p> Jonatan Gómez Fernández </p>");
			out.println("</footer>");

			out.println("</body>");
			out.println("</html>");

		}
		return;
	}

	public void getC2Acts(String plang){

		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		NodeList ListaPelisconLang=null;
		NodeList ListaPelissingLang=null;
		NodeList ListaActoresNode=null;
		NodeList ListaReparto=null;
		ArrayList<String> listaActores = new ArrayList<String>();

		try{

			for(String i : movies.keySet()){

				ListaPelissingLang=(NodeList) xPath.evaluate("/Movies/Pais[@lang='"+plang+"']/Pelicula/Reparto", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
				for(int l=0;l<ListaPelissingLang.getLength();l++){
					String actores=(String) xPath.evaluate("Nombre", ListaPelissingLang.item(l), XPathConstants.STRING);
					actores=actores.trim();
					if(!listaActores.contains(actores)){
						listaActores.add(actores);
					}
				}

				ListaPelisconLang=(NodeList) xPath.evaluate("/Movies/Pais/Pelicula[contains(@langs,'"+plang+"')]/Reparto" , movies.get(i).getDocumentElement(), XPathConstants.NODESET);
				for (int k=0;k<ListaPelisconLang.getLength();k++){
					String actores=(String) xPath.evaluate("Nombre", ListaPelisconLang.item(k), XPathConstants.STRING);
					actores=actores.trim();
					if(!listaActores.contains(actores)){
						listaActores.add(actores);
					}
				}

			}

			for(String p : movies.keySet()){

				ListaActoresNode = (NodeList) xPath.evaluate("/Movies/Pais/Pelicula/Reparto" , movies.get(p).getDocumentElement(), XPathConstants.NODESET);
				for(int v=0;v<ListaActoresNode.getLength();v++){
					String actorConOscar=(String) xPath.evaluate("Nombre", ListaActoresNode.item(v), XPathConstants.STRING);
					actorConOscar=actorConOscar.trim();
					String oscar=(String) xPath.evaluate("Oscar", ListaActoresNode.item(v), XPathConstants.STRING);
					if (oscar != ""){
						String ciudad=(String) xPath.evaluate("text()[normalize-space()]", ListaActoresNode.item(v), XPathConstants.STRING);
						ciudad=ciudad.trim();
						if (listaActores.contains(actorConOscar)){
							String actoryCiudad=actorConOscar+" ("+ciudad+")";
							actoresOscar.put(actoryCiudad,"-- con óscar");
						}
					}
				}
			}
			for(String c : movies.keySet()){
				ListaActoresNode = (NodeList) xPath.evaluate("/Movies/Pais/Pelicula/Reparto" , movies.get(c).getDocumentElement(), XPathConstants.NODESET);
				for(int b=0;b<ListaActoresNode.getLength();b++){
					String oscar=(String) xPath.evaluate("Oscar", ListaActoresNode.item(b), XPathConstants.STRING);
					if (oscar == ""){
						String actorSinOscar=(String) xPath.evaluate("Nombre", ListaActoresNode.item(b), XPathConstants.STRING);
						actorSinOscar=actorSinOscar.trim();
						String ciudad=(String) xPath.evaluate("text()[normalize-space()]", ListaActoresNode.item(b), XPathConstants.STRING);
						ciudad=ciudad.trim();
						String actoryCiudads=actorSinOscar+" ("+ciudad+")";
						if (!actoresOscar.containsKey(actoryCiudads) && listaActores.contains(actorSinOscar)){
							actoresSinOscar.put(actoryCiudads,"-- sin óscar");

						}

					}

				}
			}
		}
		catch (Exception e){

		}
	}

	public void fase23(PrintWriter out, boolean auto, String pass,String plang,String pact) {

		getC2Paises(plang, pact);

		if (auto) {

			out.println("<?xml version='1.0' encoding='utf-8' ?>");

			if (plang ==null){

				out.println("<wrongRequest>no param:plang</wrongRequest>");
				return;

			}
			else if(pact==null){
				out.println("<wrongRequest>no param:pact</wrongRequest>");
				return;

			}
			out.println("<paises>");

			for (int i=0;i<listaPaisesaArrayList.size();i++){
				System.out.println(listaPaisesaArrayList.get(i));


				out.println("<pais lang='"+paisesIdiomas.get(listaPaisesaArrayList.get(i))+"' num='"+peliculasPaisNumero.get(listaPaisesaArrayList.get(i))+"'>"+listaPaisesaArrayList.get(i)+"</pais>");


			}

			out.println("</paises>");


		} else {

			if (plang ==null){

				out.println("Falta el parámetro plang");
				return;

			}
			else if(pact==null){
				out.println("Falta el parámetro pact");
				return;

			}

			out.println("<html>");
			out.println("<head>");
			out.println("<link rel='stylesheet' type='text/css' href='mml.css'/>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h1>Servicio de consulta de películas</h1>");
			out.println("<h2>Idioma="+plang+", Actor/Actriz="+pact+"</h2>");
			out.println("<h2>Selecciona un actor:</h2>");
			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='hidden' name='plang' value='" + plang + "'/>");
			out.println("<input type='hidden' name='pact' value='" + pact + "'/>");
			out.println("<input type='hidden' name='pfase' value='24' />");

			int numeracion=1;
			for (int i=0;i<listaPaisesaArrayList.size();i++){

				if (peliculasPaisNumero.get(listaPaisesaArrayList.get(i)) ==1){
					out.println("<input type='radio' name='ppais' value='"+listaPaisesaArrayList.get(i)+"'checked/>"+numeracion+".-"+listaPaisesaArrayList.get(i)+
					" ("+peliculasPaisNumero.get(listaPaisesaArrayList.get(i))+" película) -- idioma por defecto='"+paisesIdiomas.get(listaPaisesaArrayList.get(i))+"'<br>");
				}
				else{
					out.println("<input type='radio' name='ppais' value='"+listaPaisesaArrayList.get(i)+"'checked/>"+numeracion+".-"+listaPaisesaArrayList.get(i)+
					" ("+peliculasPaisNumero.get(listaPaisesaArrayList.get(i))+" películas) -- idioma por defecto='"+paisesIdiomas.get(listaPaisesaArrayList.get(i))+"'<br>");
				}


				numeracion++;

			}
			out.println("<input type='submit' value='Enviar'/><br>");

			out.println("</form>");

			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='pfase' value='22' />");
			out.println("<input type='hidden' name='p' value='"+pass+"'/>");
			out.println("<input type='hidden' name='plang' value='"+plang+"'/>");
			out.println("<input type='hidden' name='pact' value='"+pact+"'/>");
			out.println("<input type='submit' value='Atrás'/>");
			out.println("</form>");

			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='pfase' value='01'/>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='submit' value='Inicio'/>");
			out.println("</form>");

			out.println("<footer class='pie-pag'>");
			out.println("<p> Jonatan Gómez Fernández </p>");
			out.println("</footer>");

			out.println("</body>");
			out.println("</html>");

		}
		return;
	}

	static final Comparator<String> ordennum =  new Comparator<String>() {
		public int compare(String uno, String dos){
			Integer s1=peliculasPaisNumero.get(uno);
			Integer s2=peliculasPaisNumero.get(dos);
			return s2.compareTo(s1);
		}
	};

	public void getC2Paises (String plang,String pact){

		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		NodeList ListaPaises=null;
		NodeList ListaPelisPorPais=null;
		NodeList ListaPelissingLang=null;
		NodeList ListaPelisconLang=null;
		NodeList NumerodePelis=null;
		NodeList TieneLangs=null;
		String pais="";
		String mirarplang="";
		int cuentapelis=0;
		NodeList ComprobarPlang=null;
		ArrayList<String> listaActores = new ArrayList<String>();
		String idiomaPorDefecto="";

		try{

			for(String i : movies.keySet()){
				System.out.println("ITERACION"+i);

				ListaPaises=(NodeList) xPath.evaluate("/Movies/Pais", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
				for (int q=0;q<ListaPaises.getLength();q++){

					pais= (String) xPath.evaluate("@pais", ListaPaises.item(q), XPathConstants.STRING);
					idiomaPorDefecto=(String) xPath.evaluate("@lang", ListaPaises.item(q), XPathConstants.STRING);
					paisesIdiomas.put(pais, idiomaPorDefecto);
					ListaPelisPorPais  = (NodeList) xPath.evaluate("/Movies/Pais[@pais='"+pais+"']", movies.get(i).getDocumentElement(), XPathConstants.NODESET);

					for (int w=0;w<ListaPelisPorPais.getLength();w++){

						ListaPelisconLang=(NodeList) xPath.evaluate("/Movies/Pais[@pais='"+pais+"']/Pelicula[contains(@langs,'"+plang+"')]/Reparto" , movies.get(i).getDocumentElement(), XPathConstants.NODESET);
						for (int k=0;k<ListaPelisconLang.getLength();k++){
							String actores=(String) xPath.evaluate("Nombre", ListaPelisconLang.item(k), XPathConstants.STRING);
							actores=actores.trim();
							System.out.println("actor arriba	"+actores);
							if(!listaActores.contains(actores)){
								listaActores.add(actores);
							}
						}
						TieneLangs=(NodeList) xPath.evaluate("/Movies/Pais[@pais='"+pais+"']/Pelicula", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
						for (int t=0;t<TieneLangs.getLength();t++){
							String langs=(String) xPath.evaluate("@langs", TieneLangs.item(t), XPathConstants.STRING);
							if (langs.length()<2){

								ListaPelissingLang=(NodeList) xPath.evaluate("/Movies/Pais[@lang='"+plang+"' and @pais='"+pais+"']/Pelicula/Reparto", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
								for(int l=0;l<ListaPelissingLang.getLength();l++){
									String actores=(String) xPath.evaluate("Nombre", ListaPelissingLang.item(l), XPathConstants.STRING);
									actores=actores.trim();
									System.out.println("actor abajo	"+actores);
									if(!listaActores.contains(actores)){
										listaActores.add(actores);
									}
								}
							}
						}

						if (listaActores.contains(pact.trim()) && !listaPaisesaArrayList.contains(pais)){
							System.out.println("pais dentro"+pais);
							listaPaisesaArrayList.add(pais);

						}
						listaActores.clear();

					}
				}

			}


			for(int i=0;i<listaPaisesaArrayList.size();i++){
				cuentapelis=0;
				for(String o : movies.keySet()){
					NumerodePelis=(NodeList) xPath.evaluate("/Movies/Pais[@pais='"+listaPaisesaArrayList.get(i)+"']/Pelicula", movies.get(o).getDocumentElement(), XPathConstants.NODESET);
					for (int t=0;t<NumerodePelis.getLength();t++){
						String pelis=(String) xPath.evaluate("Titulo", NumerodePelis.item(t), XPathConstants.STRING);
						if (pelis!=""){
							cuentapelis++;

						}

						peliculasPaisNumero.put(listaPaisesaArrayList.get(i),cuentapelis);

					}
				}

			}

			for (Map.Entry<String, Integer> entry : peliculasPaisNumero.entrySet()) {

				System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
			}

			Collections.sort(listaPaisesaArrayList);
			Collections.sort(listaPaisesaArrayList,ordennum);
			System.out.println("ie2"+listaPaisesaArrayList);

		}
		catch(Exception e){

		}

	}

	public void fase24(PrintWriter out, boolean auto, String pass,String plang,String pact,String ppais) {
		getC2peliculas(plang, pact, ppais);

		if (auto) {

			out.println("<?xml version='1.0' encoding='utf-8' ?>");

			if (plang ==null){

				out.println("<wrongRequest>no param:plang</wrongRequest>");
				return;

			}
			else if(pact==null){
				out.println("<wrongRequest>no param:pact</wrongRequest>");
				return;

			}
			else if (ppais==null){
				out.println("<wrongRequest>no param:ppais</wrongRequest>");
				return;

			}
			out.println("<titulos>");

			for (Map.Entry<String, String> entry : pelisIPfinal.entrySet()) {


				out.println("<titulo ip='"+entry.getKey()+"'"+">"+entry.getValue()+"</titulo>");

			}

			out.println("</titulos>");

		}

		else {

			if (plang ==null){

				out.println("Falta el parámetro plang");
				return;

			}
			else if(pact==null){
				out.println("Falta el parámetro pact");
				return;

			}
			else if (ppais==null){
				out.println("Falta el parámetro ppais");
				return;

			}

			out.println("<html>");
			out.println("<head>");
			out.println("<link rel='stylesheet' type='text/css' href='mml.css'/>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h1>Servicio de consulta de películas</h1>");
			out.println("<h2> Idioma="+plang+",Actor/Actriz="+pact.trim()+",Pais="+ppais+"</h2>");
			out.println("<h2>Estas son sus películas</h2>");
			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='hidden' name='pfase' value='23' />");
			out.println("<input type='hidden' name='plang' value='"+plang+"'/>");
			out.println("<input type='hidden' name='pact' value='"+pact+"'/>");
			out.println("<ul>");
			int numeracion=1;
			for (Map.Entry<String, String> entry : pelisIPfinal.entrySet()) {
				out.println("<li>"+numeracion+".-<B>Película</B>="+entry.getValue()+", <B>IP</B>="+entry.getKey()+"</li>");

				numeracion++;
			}


			out.println("</ul>");


			out.println("<input type='submit' value='Atrás'/>");
			out.println("</form>");

			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='pfase' value='01'/>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='submit' value='Inicio'/>");
			out.println("</form>");

			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='hidden' name='plang' value='" + plang + "'/>");
			out.println("<input type='hidden' name='pact' value='" + pact + "'/>");
			out.println("<input type='hidden' name='ppais' value='"+ppais+"'/>");
			out.println("<input type='hidden' name='pfase' value='25' />");
			out.println("<input type='submit' value='Next'/>");
			out.println("</form>");

			out.println("<footer class='pie-pag'>");
			out.println("<p> Jonatan Gómez Fernández </p>");
			out.println("</footer>");

			out.println("</body>");
			out.println("</html>");

		}
		return;

	}


	public void getC2peliculas(String plang,String pact,String ppais){


		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		NodeList ListaPelisconLang=null;
		NodeList ListaPelissinLang=null;
		NodeList comprobarLang=null;
		NodeList ListaComprobar=null;
		NodeList ListaReparto=null;


		try{

			for(String i : movies.keySet()){

				comprobarLang=(NodeList) xPath.evaluate("/Movies/Pais/Pelicula", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
				for(int j=0;j<comprobarLang.getLength();j++){
					String langs=(String) xPath.evaluate("@langs", comprobarLang.item(j), XPathConstants.STRING);
					String pelis=(String) xPath.evaluate("Titulo", comprobarLang.item(j), XPathConstants.STRING);
					String ip=(String) xPath.evaluate("@ip", comprobarLang.item(j), XPathConstants.STRING);

					if (langs !=""){
						ListaPelisconLang=(NodeList) xPath.evaluate("/Movies/Pais[@pais='"+ppais+"']/Pelicula[contains(@langs,'"+plang+"')]", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
						for (int l=0;l<ListaPelisconLang.getLength();l++){

							System.out.println(pelis+"  "+ip);
							ListaComprobar=(NodeList) xPath.evaluate("/Movies/Pais[@pais='"+ppais+"']/Pelicula[contains(@langs,'"+plang+"') and @ip='"+ip+"']/Reparto", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
							for (int s=0;s<ListaComprobar.getLength();s++){
								String nombre=(String) xPath.evaluate("Nombre", ListaComprobar.item(s), XPathConstants.STRING);
								if (nombre.equals(pact.trim())){
									pelisIPfinal.put(ip, pelis);

								}

							}
						}

					}
					else if(langs == ""){

						ListaPelissinLang=(NodeList) xPath.evaluate("/Movies/Pais[@lang='"+plang+"']/Pelicula", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
						for (int l=0;l<ListaPelissinLang.getLength();l++){
							ListaComprobar=(NodeList) xPath.evaluate("/Movies/Pais[@pais='"+ppais+"']/Pelicula[@ip='"+ip+"']/Reparto", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
							for (int s=0;s<ListaComprobar.getLength();s++){
								String nombre=(String) xPath.evaluate("Nombre", ListaComprobar.item(s), XPathConstants.STRING);
								if (nombre.equals(pact.trim())){
									pelisIPfinal.put(ip, pelis);
								}

							}

						}
					}
				}
			}

			for (Map.Entry<String, String> entry : pelisIPfinal.entrySet()) {

				System.out.println("el treemap:"+"Key: " + entry.getKey() + ". Value: " + entry.getValue());
			}
		}
		catch(Exception e){
		}
	}

	public void fase25(PrintWriter out, boolean auto, String pass,String plang,String pact,String ppais){

		if(color_examen.equals("rojo")){
			color_examen_traducido="red";
			// out.println("<p class='color_rojo'>prueba1</p>");

		}

		if (color_examen.equals("verde")){
			color_examen_traducido="green";
			// out.println("<p class='color_verde'>prueba2</p>");
		}

		if (auto) {

			out.println("<?xml version='1.0' encoding='utf-8' ?>");

			if (plang ==null){

				out.println("<wrongRequest>no param:plang</wrongRequest>");
				return;

			}
			else if(pact==null){
				out.println("<wrongRequest>no param:pact</wrongRequest>");
				return;

			}
			else if (ppais==null){
				out.println("<wrongRequest>no param:ppais</wrongRequest>");
				return;

			}
			out.println("<titulos>");

			for (Map.Entry<String, String> entry : pelisIPfinal.entrySet()) {


				out.println("<titulo ip='"+entry.getKey()+"'"+">"+entry.getValue()+"</titulo>");
			}

			out.println("</titulos>");

		}

		else {

			if (plang ==null){

				out.println("Falta el parámetro plang");
				return;

			}
			else if(pact==null){
				out.println("Falta el parámetro pact");
				return;

			}
			else if (ppais==null){
				out.println("Falta el parámetro ppais");
				return;

			}

			out.println("<html>");
			out.println("<head>");
			out.println("<link rel='stylesheet' type='text/css' href='mml.css'/>");
			out.println("</head>");
			out.println("<body>");
			out.println("<h1>Servicio de consulta de películas</h1>");
			out.println("<h2> Idioma="+plang+",Actor/Actriz="+pact.trim()+",Pais="+ppais+"</h2>");
			out.println("<h2>Estas son sus películas</h2>");


			out.println("<h3>"+idioma_examen+" </h3>");
			out.println("<h3>"+pais_examen+" </h3>");
			out.println("<h3>"+color_examen+" </h3>");
			out.println("<p style='color:"+color_examen_traducido+"'>POZACO</p>");

			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='hidden' name='plang' value='"+plang+"'/>");
			out.println("<input type='hidden' name='pact' value='"+pact+"'/>");
			out.println("<input type='hidden' name='ppais' value='"+ppais+"'/>");
			out.println("<input type='hidden' name='pfase' value='24' />");
			out.println("<input type='submit' value='Atras'/>");
			out.println("</form>");

			out.println("<form action='' method='get'>");
			out.println("<input type='hidden' name='pfase' value='01'/>");
			out.println("<input type='hidden' name='p' value='" + pass + "'/>");
			out.println("<input type='submit' value='Inicio'/>");
			out.println("</form>");

			out.println("<footer class='pie-pag'>");
			out.println("<p> Jonatan Gómez Fernández </p>");
			out.println("</footer>");

			out.println("</body>");
			out.println("</html>");

		}


		for(String i : movies.keySet()){
			System.out.println("ITERACION"+i);

			ListaPaises=(NodeList) xPath.evaluate("/Movies/Pais", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
			for (int q=0;q<ListaPaises.getLength();q++){

				idiomaPorDefecto=(String) xPath.evaluate("@lang", ListaPaises.item(q), XPathConstants.STRING);
				paisesIdiomas.put(pais, idiomaPorDefecto);
				ListaPelisPorPais  = (NodeList) xPath.evaluate("/Movies/Pais[@pais='"+pais+"']", movies.get(i).getDocumentElement(), XPathConstants.NODESET);

				for (int w=0;w<ListaPelisPorPais.getLength();w++){

					ListaPelisconLang=(NodeList) xPath.evaluate("/Movies/Pais[@pais='"+pais+"']/Pelicula[contains(@langs,'"+plang+"')]/Reparto" , movies.get(i).getDocumentElement(), XPathConstants.NODESET);
					for (int k=0;k<ListaPelisconLang.getLength();k++){
						String actores=(String) xPath.evaluate("Nombre", ListaPelisconLang.item(k), XPathConstants.STRING);
						actores=actores.trim();
						System.out.println("actor arriba	"+actores);
						if(!listaActores.contains(actores)){
							listaActores.add(actores);
						}
					}
					TieneLangs=(NodeList) xPath.evaluate("/Movies/Pais[@pais='"+pais+"']/Pelicula", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
					for (int t=0;t<TieneLangs.getLength();t++){
						String langs=(String) xPath.evaluate("@langs", TieneLangs.item(t), XPathConstants.STRING);
						if (langs.length()<2){

							ListaPelissingLang=(NodeList) xPath.evaluate("/Movies/Pais[@lang='"+plang+"' and @pais='"+pais+"']/Pelicula/Reparto", movies.get(i).getDocumentElement(), XPathConstants.NODESET);
							for(int l=0;l<ListaPelissingLang.getLength();l++){
								String actores=(String) xPath.evaluate("Nombre", ListaPelissingLang.item(l), XPathConstants.STRING);
								actores=actores.trim();
								System.out.println("actor abajo	"+actores);
								if(!listaActores.contains(actores)){
									listaActores.add(actores);
								}
							}
						}
					}

					if (listaActores.contains(pact.trim()) && !listaPaisesaArrayList.contains(pais)){
						System.out.println("pais dentro"+pais);
						listaPaisesaArrayList.add(pais);

					}
					listaActores.clear();

				}
			}

		}
		return;

	}

}
