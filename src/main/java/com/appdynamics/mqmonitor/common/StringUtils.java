/**
 *
 */
package com.appdynamics.mqmonitor.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



class XmlElement {
	public String elementName;
	public List<XmlElementInstance> instances = new ArrayList<XmlElementInstance>();
}

class XmlElementInstance {
	public int depth;
	public int startTagEndPos;
	public boolean foundEndingTag = false;
}

/**
 * @author James Schneider
 *
 */
public class StringUtils {

	private static final String HTML_TAG_AMP = "&amp;";
	private static final String HTML_TAG_LESS_THAN = "&lt;";
	private static final String HTML_TAG_GTR_THAN = "&gt;";
	private static final String TAG_AMP = "&";

	private static final String TAG_START_LEFT_BRACKET = "<";
	private static final String TAG_END_LEFT_BRACKET = "</";
	private static final String TAG_END_RIGHT_BRACKET = "/>";
	private static final String TAG_RIGHT_BRACKET = ">";
	private static final String TAG_SLASH = "/";
	private static final String COMMENT_TAG_EX = "!";
	private static final String TAG_SPACE = " ";
	private static final String TAG_LINE_FEED = System.getProperty("line.separator");
	private static final String STRING_DATE_FORMAT = "yyyy-MM-dd' T 'HH:mm:ss";
	private static final String FILE_STAMP_STRING_DATE_FORMAT = "yyyy-MM-dd-'T'-HH-mm-ss";

	private static final String CRLF_WIN = "\r\n";
	private static final String CRLF_UNIX = "\n";
	private static final String CRLF_MAC = "\r";

	private static final List<String> ALPHA_CHARS = new ArrayList<String>(26);
	private static final List<String> NUMERIC_CHARS = new ArrayList<String>(10);


	static {
		ALPHA_CHARS.add("A");
		ALPHA_CHARS.add("B");
		ALPHA_CHARS.add("C");
		ALPHA_CHARS.add("D");
		ALPHA_CHARS.add("E");
		ALPHA_CHARS.add("F");
		ALPHA_CHARS.add("G");
		ALPHA_CHARS.add("H");
		ALPHA_CHARS.add("I");
		ALPHA_CHARS.add("J");
		ALPHA_CHARS.add("K");
		ALPHA_CHARS.add("L");
		ALPHA_CHARS.add("M");
		ALPHA_CHARS.add("N");
		ALPHA_CHARS.add("O");
		ALPHA_CHARS.add("P");
		ALPHA_CHARS.add("Q");
		ALPHA_CHARS.add("R");
		ALPHA_CHARS.add("S");
		ALPHA_CHARS.add("T");
		ALPHA_CHARS.add("U");
		ALPHA_CHARS.add("V");
		ALPHA_CHARS.add("W");
		ALPHA_CHARS.add("X");
		ALPHA_CHARS.add("Y");
		ALPHA_CHARS.add("Z");

		NUMERIC_CHARS.add("0");
		NUMERIC_CHARS.add("1");
		NUMERIC_CHARS.add("2");
		NUMERIC_CHARS.add("3");
		NUMERIC_CHARS.add("4");
		NUMERIC_CHARS.add("5");
		NUMERIC_CHARS.add("6");
		NUMERIC_CHARS.add("7");
		NUMERIC_CHARS.add("8");
		NUMERIC_CHARS.add("9");

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {


			String filePath = "C:/workspaces/dispauto/raft-configuration-debug.xml";

			String xml = StringUtils.getFileAsString(filePath);

			//System.out.println(xml+ System.getProperty("line.separator")+ System.getProperty("line.separator"));

			xml = StringUtils.formatXml(xml, "IMH_Configurations", 2);

			System.out.println(xml);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public static String escapeHtml(String html) {
		html = StringUtils.replaceAll(html, TAG_AMP, HTML_TAG_AMP);
		html = StringUtils.replaceAll(html, TAG_START_LEFT_BRACKET, HTML_TAG_LESS_THAN);
		html = StringUtils.replaceAll(html, TAG_RIGHT_BRACKET, HTML_TAG_GTR_THAN);
		return html;
	}

	public static String unescapeHtml(String html) {
		html = StringUtils.replaceAll(html, HTML_TAG_AMP, TAG_AMP);
		html = StringUtils.replaceAll(html, HTML_TAG_LESS_THAN, TAG_START_LEFT_BRACKET);
		html = StringUtils.replaceAll(html, HTML_TAG_GTR_THAN, TAG_RIGHT_BRACKET);
		return html;
	}

	public static String getNewLine() {
		return System.getProperty("line.separator");
	}

	private static boolean stringValueAllZeros(String val) {
		int tmpInt;
		try {
			tmpInt = Integer.parseInt(val.trim());
			if (tmpInt > 0) {
				return false;
			} else {
				return true;
			}
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	public static boolean stringValueAllZerosNotEmpty(String val) {
		if (StringUtils.isEmpty(val)) return false;
		return StringUtils.stringValueAllZeros(val);
	}
	public static boolean stringValueAllZerosOrEmpty(String val) {
		if (StringUtils.isEmpty(val)) return true;
		return StringUtils.stringValueAllZeros(val);
	}

	/**
	 * Formats a piece of XML, making it more user readable by indenting the
	 * start and end tags of the outer most element and the rest of the nested
	 * elements contained within.
	 *
	 * Below is an example of a piece of XML passed in to the method:
	 *
	 * <FirstElement name="shutdownDatabase" type="java.lang.String" value="something">
	 * <SecondElement id="s2vc1" angle="left">  <ThirdElements> <ThirdElement> This is the first instance of the third element </ThirdElement>
	 * <ThirdElement> This is the second instance of the third element </ThirdElement> <ThirdElement> This is the third instance of the third element
	 * </ThirdElement>    <ThirdElements></SecondElement></FirstElementname>
	 *
	 * Here is what it would look like after being formatted with an indent of 2:
	 *
     * <FirstElement name="shutdownDatabase" type="java.lang.String" value="something">
     *   <SecondElement id="s2vc1" angle="left">
     *     <ThirdElements>
     *       <ThirdElement> This is the first instance of the third element </ThirdElement>
     *       <ThirdElement> This is the second instance of the third element </ThirdElement>
     *       <ThirdElement> This is the third instance of the third element</ThirdElement>
     *     </ThirdElements>
     *   </SecondElement>
     * </FirstElement>
     *
	 * @param xml - the piece of XML to be formatted
	 * @param outerElement - the name of the outer most element without the element brackets
	 * @param indent - the number of spaces used for the indentation
	 * @return - the formatted, indented XML
	 */
	public static String formatXml(String xml, String outerElement, int indent) {
		StringBuffer buff = new StringBuffer();

		try {
			// first remove all the carriage return line feeds
			// regardless of platform
			xml = StringUtils.replaceAll(xml, CRLF_WIN, "");
			xml = StringUtils.replaceAll(xml, CRLF_UNIX, "");
			xml = StringUtils.replaceAll(xml, CRLF_MAC, "");

			int totalChars = xml.length();
			String chr;
			int lastBegin = 0;
			StringBuffer strBuff = new StringBuffer();
			boolean foundEndBracket = false;
			//boolean foundStartBracket = false;
			for (int i = 0; i < totalChars; i++) {
				chr = xml.substring(lastBegin, lastBegin + 1);
				lastBegin++;

				if (chr.equals(TAG_RIGHT_BRACKET)) {
					foundEndBracket = true;
				}
				if (chr.equals(TAG_START_LEFT_BRACKET) && foundEndBracket) {
					foundEndBracket = false;
				}

				if (foundEndBracket && chr.equals(" ")) {

				} else {
					strBuff.append(chr);
				}
			}

			//System.out.println(strBuff.toString());

			xml = strBuff.toString();
			strBuff = null;

			Map<String, XmlElement> elements = new HashMap<String, XmlElement>();

			boolean notDone = true;
			int currentDepth = 0;

			int beginPos = 0;
			int tagBeginPos;
			int tagEndPos;
			int nextTagBeginPos;
			int tempPos;

			String tempString;
			String tempBeginTag = "";
			String tempEndTag = "";
			boolean firstElement = true;
			boolean isStartAlsoEnd = false;
			XmlElement xmlElement = null;
			XmlElementInstance elementInstance = null;

			List<XmlElementInstance> instances = null;
			XmlElementInstance lastInstance = null;
			int lastInstancePos = 0;

			while (notDone) {

				isStartAlsoEnd = false;
				// find the starting position of either the first / outermost tag
				// or the next tag and set the 'tagBeginPos' to that value
				if (firstElement) {
					// getting the position of "<"
					tagBeginPos = xml.indexOf((TAG_START_LEFT_BRACKET + outerElement), beginPos);
				} else {
					// getting the position of "<"
					tagBeginPos = xml.indexOf((TAG_START_LEFT_BRACKET), beginPos);
				}


				// find the ending position of the tag we just found.
				// this is the position of the matching right bracket
				// for the left bracket we just found.

				// getting the position of ">"
				tagEndPos = xml.indexOf(TAG_RIGHT_BRACKET, tagBeginPos);


				// find the next character after the "<" to see what kind
				// of tag we are dealing with.
				// getting the value of what if just right of the left bracket "<?"
				tempString = xml.substring((tagBeginPos + 1), (tagBeginPos + 2));
				//System.out.println("<? " + tempString);


				// if it is "/" then we are dealing with an ending tag
				if (tempString.equals(TAG_SLASH)) {

					// found an ending element tag
					tagBeginPos = xml.indexOf((TAG_END_LEFT_BRACKET), beginPos);
					tagEndPos = xml.indexOf(TAG_RIGHT_BRACKET, tagBeginPos);
					tempEndTag = xml.substring(tagBeginPos, tagEndPos + 1);

					tempString = tempEndTag.substring(2);
					tempString = tempString.substring(0, tempString.length() - 1);

					xmlElement = elements.get(tempString);

					instances = xmlElement.instances;
					lastInstance = null;
					lastInstancePos = 0;

					for (XmlElementInstance instance : instances) {
						if (instance.startTagEndPos > lastInstancePos && instance.foundEndingTag == false) {
							lastInstancePos = instance.startTagEndPos;
							lastInstance = instance;
						}
					}

					// now add the indent and the ending element tag
					for (int i = 0; i < lastInstance.depth; i++) {
						for (int j = 0; j < indent; j++) {
							buff.append(TAG_SPACE);
						}
					}

					//System.out.println("ENDING TAG - Adding to buff: " + tempEndTag);
					buff.append(tempEndTag.trim());
					buff.append(TAG_LINE_FEED);

					lastInstance.foundEndingTag = true;
					beginPos = tagEndPos + 1;
					currentDepth--;

					if (outerElement.equals(tempString)) {
						notDone = false;
						//System.out.println("Number of Elements: " + elements.size());
						elements = null;
					}


				// if it is a "!" then we are dealing with a comment
				} else if (tempString.equals(COMMENT_TAG_EX)) {

					// this gets the comment tag with no brackets
					//tempBeginTag = xml.substring((tagBeginPos + 1), tagEndPos);
					tempString = xml.substring((tagBeginPos + 1), tagEndPos);

					xmlElement =  null;
					xmlElement = elements.get(tempString);
					if (xmlElement == null) {
						xmlElement = new XmlElement();
						xmlElement.elementName = tempString;
						//System.out.println("COMMENT TAG - Adding Element " + tempString + " to elements map");
						elements.put(tempString, xmlElement);
					}
					elementInstance = new XmlElementInstance();

					elementInstance.startTagEndPos = tagEndPos + 1;
					elementInstance.depth = currentDepth;

					tempBeginTag = xml.substring(tagBeginPos, (tagEndPos + 1));

					// now add the indent and the starting element tag
					for (int i = 0; i < currentDepth; i++) {
						for (int j = 0; j < indent; j++) {
							buff.append(TAG_SPACE);
						}
					}
					//System.out.println("COMMENT TAG - Adding to buff: " + tempBeginTag);
					buff.append(tempBeginTag.trim());

					elementInstance.foundEndingTag = true;
					buff.append(TAG_LINE_FEED);

					// TODO get rid of the "+ 1" to see if this helps
					nextTagBeginPos = tagEndPos + 1;

					beginPos = nextTagBeginPos;
					xmlElement.instances.add(elementInstance);


					// ####################################################################

//					instances = xmlElement.instances;
//					lastInstance = null;
//					lastInstancePos = 0;
//
//					for (XmlElementInstance instance : instances) {
//						if (instance.startTagEndPos > lastInstancePos && instance.foundEndingTag == false) {
//							lastInstancePos = instance.startTagEndPos;
//							lastInstance = instance;
//						}
//					}

					// #####################################################################


				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				// else we are dealing with a beginning tag that either has a separate
			    // ending tag or is a marker tag that has no content (e.g. see below "isStartAlsoEnd")
				} else {

					// this gets the tag with no brackets but with any attributes
					tempBeginTag = xml.substring((tagBeginPos + 1), tagEndPos);
					//System.out.println("STARTING TAG - tempBeginTag: " + tempBeginTag);
					tempPos = tempBeginTag.indexOf(TAG_SPACE);

					isStartAlsoEnd = false;
					String lastSlash = xml.substring((tagEndPos - 1), (tagEndPos));
					//System.out.println("STARTING TAG - lastSlash: " + lastSlash);
					if (lastSlash.equals(TAG_SLASH)) {
						//tempBeginTag.indexOf(TAG_SLASH) > -1
						isStartAlsoEnd = true;
					}

					if (tempPos > -1) {
						// this tag has attributes so we need to extract just the tag name
						tempPos = xml.indexOf(TAG_SPACE, (tagBeginPos + 1));
						if (isStartAlsoEnd) {
							tempString = xml.substring((tagBeginPos + 1), (tempPos - 1));
						} else {
							tempString = xml.substring((tagBeginPos + 1), tempPos);
						}
						//tempString = tempString.trim();

					} else {
						// this tag has no attributes so we just use the existing end position
						if (isStartAlsoEnd) {
							tempString = xml.substring((tagBeginPos + 1), (tagEndPos - 1));
						} else {
							tempString = xml.substring((tagBeginPos + 1), tagEndPos);
						}
						//tempString = tempString.trim();
					}


					// found a starting element tag
					xmlElement =  null;
					xmlElement = elements.get(tempString);
					if (xmlElement == null) {
						xmlElement = new XmlElement();
						xmlElement.elementName = tempString;
						//System.out.println("STARTING TAG - Adding Element " + tempString + " to elements map");
						elements.put(tempString, xmlElement);
					}
					elementInstance = new XmlElementInstance();

					elementInstance.startTagEndPos = tagEndPos + 1;
					elementInstance.depth = currentDepth;

					// this gets the tag with the brackets around it
					//tempBeginTag = xml.substring(tagBeginPos, (tagEndPos + 1));

					if (isStartAlsoEnd) {
						tempBeginTag = xml.substring(tagBeginPos, (tagEndPos - 1)).trim() + TAG_RIGHT_BRACKET;
					} else {
						tempBeginTag = xml.substring(tagBeginPos, (tagEndPos + 1));
					}

					//System.out.println("tempBeginTag 2: " + tempBeginTag);
					//System.out.println("tempEndTag 1: " + tempEndTag);

					if (firstElement) {
						firstElement = false;
					}

					// now add the indent and the starting element tag
					for (int i = 0; i < currentDepth; i++) {
						for (int j = 0; j < indent; j++) {
							buff.append(TAG_SPACE);
						}
					}

					if (isStartAlsoEnd) {
						//System.out.println("STARTING TAG* - Adding to buff: " + tempBeginTag);
						buff.append(tempBeginTag.trim());

						elementInstance.foundEndingTag = true;

						tempString = TAG_END_LEFT_BRACKET + xmlElement.elementName.trim() + TAG_RIGHT_BRACKET;
						//System.out.println("STARTING TAG* - Adding to buff: " + tempString);
						buff.append(tempString.trim());
						buff.append(TAG_LINE_FEED);

						// TODO get rid of the "+ 1" to see if this helps
						nextTagBeginPos = tagEndPos + 1;

					} else {

						//System.out.println("STARTING TAG - Adding to buff: " + tempBeginTag);
						buff.append(tempBeginTag.trim());

						// then check to see if this element contains other elements
						tempEndTag = TAG_END_LEFT_BRACKET + xmlElement.elementName.trim() + TAG_RIGHT_BRACKET;

						//System.out.println("STARTING TAG - tempEndTag: " + tempEndTag);
						//System.out.println("STARTING TAG - tagEndPos + 1: " + (tagEndPos + 1));


						tempPos = xml.indexOf(tempEndTag, (tagEndPos + 1));

						tempString = xml.substring((tagEndPos + 1), tempPos);
						//System.out.println("STARTING TAG - tempString: " + tempString);
						if (tempString.indexOf(TAG_END_LEFT_BRACKET) > -1 || tempString.indexOf(TAG_END_RIGHT_BRACKET) > -1) {
							//System.out.println("Tag " + xmlElement.elementName + " has nested elements");
							// the current element has other elements
							elementInstance.foundEndingTag = false;
							// increase the indent here
							currentDepth++;
							buff.append(TAG_LINE_FEED);
							nextTagBeginPos = elementInstance.startTagEndPos;

						} else {
							//System.out.println("Tag " + xmlElement.elementName + " has no nested elements");

							// the current element has other no other elements
							elementInstance.foundEndingTag = true;
							tempString = xml.substring(elementInstance.startTagEndPos, tempPos + xmlElement.elementName.length() + 3);
							//System.out.println("STARTING TAG - Adding to buff: " + tempString);
							buff.append(tempString.trim());
							buff.append(TAG_LINE_FEED);

							// here we are trying to find the index where the
							// starting tag ends inside the xml string, this
							// will be the index we start searching from when
							// we loop next
							nextTagBeginPos = xml.indexOf(tempEndTag, tempPos - 1) + tempEndTag.length();
						}

					}


					beginPos = nextTagBeginPos;
					xmlElement.instances.add(elementInstance);


				}
			}

		} catch (Throwable ex) {
			ex.printStackTrace();
			//System.out.println(buff.toString());
		}

		return buff.toString();

	}

	/**
	 * Formats a piece of XML, making it more user readable by indenting the
	 * start and end tags of the outer most element and the rest of the nested
	 * elements contained within.
	 *
	 * Below is an example of a piece of XML passed in to the method:
	 *
	 * <FirstElement name="shutdownDatabase" type="java.lang.String" value="something">
	 * <SecondElement id="s2vc1" angle="left">  <ThirdElements> <ThirdElement> This is the first instance of the third element </ThirdElement>
	 * <ThirdElement> This is the second instance of the third element </ThirdElement> <ThirdElement> This is the third instance of the third element
	 * </ThirdElement>    <ThirdElements></SecondElement></FirstElementname>
	 *
	 * Here is what it would look like after being formatted with an indent of 2:
	 *
     * <FirstElement name="shutdownDatabase" type="java.lang.String" value="something">
     *   <SecondElement id="s2vc1" angle="left">
     *     <ThirdElements>
     *       <ThirdElement> This is the first instance of the third element </ThirdElement>
     *       <ThirdElement> This is the second instance of the third element </ThirdElement>
     *       <ThirdElement> This is the third instance of the third element</ThirdElement>
     *     </ThirdElements>
     *   </SecondElement>
     * </FirstElement>
     *
	 * @param xml - the piece of XML to be formatted
	 * @param outerElement - the name of the outer most element without the element brackets
	 * @param indent - the number of spaces used for the indentation
	 * @return - the formatted, indented XML
	 */
	public static List<String> formatXmlAsList(String xml, String outerElement, int indent) {
		//StringBuffer buffr = new StringBuffer();
		List<String> buffList = new ArrayList<String>();
		StringBuffer indentBuff;

		try {
			// first remove all the carriage return line feeds
			// regardless of platform
			xml = StringUtils.replaceAll(xml, CRLF_WIN, "");
			xml = StringUtils.replaceAll(xml, CRLF_UNIX, "");
			xml = StringUtils.replaceAll(xml, CRLF_MAC, "");

			int totalChars = xml.length();
			String chr;
			int lastBegin = 0;
			StringBuffer strBuff = new StringBuffer();
			boolean foundEndBracket = false;
			//boolean foundStartBracket = false;
			for (int i = 0; i < totalChars; i++) {
				chr = xml.substring(lastBegin, lastBegin + 1);
				lastBegin++;

				if (chr.equals(TAG_RIGHT_BRACKET)) {
					foundEndBracket = true;
				}
				if (chr.equals(TAG_START_LEFT_BRACKET) && foundEndBracket) {
					foundEndBracket = false;
				}

				if (foundEndBracket && chr.equals(" ")) {

				} else {
					strBuff.append(chr);
				}
			}

			//System.out.println(strBuff.toString());

			xml = strBuff.toString();
			strBuff = null;

			Map<String, XmlElement> elements = new HashMap<String, XmlElement>();

			boolean notDone = true;
			int currentDepth = 0;

			int beginPos = 0;
			int tagBeginPos;
			int tagEndPos;
			int nextTagBeginPos;
			int tempPos;

			String tempString;
			String tempBeginTag = "";
			String tempEndTag = "";
			boolean firstElement = true;
			boolean isStartAlsoEnd = false;
			XmlElement xmlElement = null;
			XmlElementInstance elementInstance = null;

			List<XmlElementInstance> instances = null;
			XmlElementInstance lastInstance = null;
			int lastInstancePos = 0;

			while (notDone) {

				isStartAlsoEnd = false;
				// find the starting position of either the first / outermost tag
				// or the next tag and set the 'tagBeginPos' to that value
				if (firstElement) {
					// getting the position of "<"
					tagBeginPos = xml.indexOf((TAG_START_LEFT_BRACKET + outerElement), beginPos);
				} else {
					// getting the position of "<"
					tagBeginPos = xml.indexOf((TAG_START_LEFT_BRACKET), beginPos);
				}


				// find the ending position of the tag we just found.
				// this is the position of the matching right bracket
				// for the left bracket we just found.

				// getting the position of ">"
				tagEndPos = xml.indexOf(TAG_RIGHT_BRACKET, tagBeginPos);


				// find the next character after the "<" to see what kind
				// of tag we are dealing with.
				// getting the value of what if just right of the left bracket "<?"
				tempString = xml.substring((tagBeginPos + 1), (tagBeginPos + 2));
				//System.out.println("<? " + tempString);


				// if it is "/" then we are dealing with an ending tag
				if (tempString.equals(TAG_SLASH)) {

					// found an ending element tag
					tagBeginPos = xml.indexOf((TAG_END_LEFT_BRACKET), beginPos);
					tagEndPos = xml.indexOf(TAG_RIGHT_BRACKET, tagBeginPos);
					tempEndTag = xml.substring(tagBeginPos, tagEndPos + 1);

					tempString = tempEndTag.substring(2);
					tempString = tempString.substring(0, tempString.length() - 1);

					xmlElement = elements.get(tempString);

					instances = xmlElement.instances;
					lastInstance = null;
					lastInstancePos = 0;

					for (XmlElementInstance instance : instances) {
						if (instance.startTagEndPos > lastInstancePos && instance.foundEndingTag == false) {
							lastInstancePos = instance.startTagEndPos;
							lastInstance = instance;
						}
					}

					// now add the indent and the ending element tag
					indentBuff = new StringBuffer();
					for (int i = 0; i < lastInstance.depth; i++) {
						for (int j = 0; j < indent; j++) {
							indentBuff.append(TAG_SPACE);
							//buff.append(TAG_SPACE);
						}
					}

					//System.out.println("ENDING TAG - Adding to buff: " + tempEndTag);
					indentBuff.append(tempEndTag.trim());
					//indentBuff.append(TAG_LINE_FEED);
					buffList.add(indentBuff.toString());

					lastInstance.foundEndingTag = true;
					beginPos = tagEndPos + 1;
					currentDepth--;

					if (outerElement.equals(tempString)) {
						notDone = false;
						//System.out.println("Number of Elements: " + elements.size());
						elements = null;
					}


				// if it is a "!" then we are dealing with a comment
				} else if (tempString.equals(COMMENT_TAG_EX)) {

					// this gets the comment tag with no brackets
					//tempBeginTag = xml.substring((tagBeginPos + 1), tagEndPos);
					tempString = xml.substring((tagBeginPos + 1), tagEndPos);

					xmlElement =  null;
					xmlElement = elements.get(tempString);
					if (xmlElement == null) {
						xmlElement = new XmlElement();
						xmlElement.elementName = tempString;
						//System.out.println("COMMENT TAG - Adding Element " + tempString + " to elements map");
						elements.put(tempString, xmlElement);
					}
					elementInstance = new XmlElementInstance();

					elementInstance.startTagEndPos = tagEndPos + 1;
					elementInstance.depth = currentDepth;

					tempBeginTag = xml.substring(tagBeginPos, (tagEndPos + 1));

					// now add the indent and the starting element tag
					indentBuff = new StringBuffer();
					for (int i = 0; i < currentDepth; i++) {
						for (int j = 0; j < indent; j++) {
							indentBuff.append(TAG_SPACE);
							//buff.append(TAG_SPACE);
						}
					}
					//System.out.println("COMMENT TAG - Adding to buff: " + tempBeginTag);
					indentBuff.append(tempBeginTag.trim());

					buffList.add(indentBuff.toString());

					elementInstance.foundEndingTag = true;
					//buff.append(TAG_LINE_FEED);

					// TODO get rid of the "+ 1" to see if this helps
					nextTagBeginPos = tagEndPos + 1;

					beginPos = nextTagBeginPos;
					xmlElement.instances.add(elementInstance);


					// ####################################################################

//					instances = xmlElement.instances;
//					lastInstance = null;
//					lastInstancePos = 0;
//
//					for (XmlElementInstance instance : instances) {
//						if (instance.startTagEndPos > lastInstancePos && instance.foundEndingTag == false) {
//							lastInstancePos = instance.startTagEndPos;
//							lastInstance = instance;
//						}
//					}

					// #####################################################################


				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				// else we are dealing with a beginning tag that either has a separate
			    // ending tag or is a marker tag that has no content (e.g. see below "isStartAlsoEnd")
				} else {

					// this gets the tag with no brackets but with any attributes
					tempBeginTag = xml.substring((tagBeginPos + 1), tagEndPos);
					//System.out.println("STARTING TAG - tempBeginTag: " + tempBeginTag);
					tempPos = tempBeginTag.indexOf(TAG_SPACE);

					isStartAlsoEnd = false;
					String lastSlash = xml.substring((tagEndPos - 1), (tagEndPos));
					//System.out.println("STARTING TAG - lastSlash: " + lastSlash);
					if (lastSlash.equals(TAG_SLASH)) {
						//tempBeginTag.indexOf(TAG_SLASH) > -1
						isStartAlsoEnd = true;
					}

					if (tempPos > -1) {
						// this tag has attributes so we need to extract just the tag name
						tempPos = xml.indexOf(TAG_SPACE, (tagBeginPos + 1));
						if (isStartAlsoEnd) {
							tempString = xml.substring((tagBeginPos + 1), (tempPos - 1));
						} else {
							tempString = xml.substring((tagBeginPos + 1), tempPos);
						}
						//tempString = tempString.trim();

					} else {
						// this tag has no attributes so we just use the existing end position
						if (isStartAlsoEnd) {
							tempString = xml.substring((tagBeginPos + 1), (tagEndPos - 1));
						} else {
							tempString = xml.substring((tagBeginPos + 1), tagEndPos);
						}
						//tempString = tempString.trim();
					}


					// found a starting element tag
					xmlElement =  null;
					xmlElement = elements.get(tempString);
					if (xmlElement == null) {
						xmlElement = new XmlElement();
						xmlElement.elementName = tempString;
						//System.out.println("STARTING TAG - Adding Element " + tempString + " to elements map");
						elements.put(tempString, xmlElement);
					}
					elementInstance = new XmlElementInstance();

					elementInstance.startTagEndPos = tagEndPos + 1;
					elementInstance.depth = currentDepth;

					// this gets the tag with the brackets around it
					//tempBeginTag = xml.substring(tagBeginPos, (tagEndPos + 1));

					if (isStartAlsoEnd) {
						tempBeginTag = xml.substring(tagBeginPos, (tagEndPos - 1)).trim() + TAG_RIGHT_BRACKET;
					} else {
						tempBeginTag = xml.substring(tagBeginPos, (tagEndPos + 1));
					}

					//System.out.println("tempBeginTag 2: " + tempBeginTag);
					//System.out.println("tempEndTag 1: " + tempEndTag);

					if (firstElement) {
						firstElement = false;
					}

					// now add the indent and the starting element tag
					indentBuff = new StringBuffer();
					for (int i = 0; i < currentDepth; i++) {
						for (int j = 0; j < indent; j++) {
							indentBuff.append(TAG_SPACE);
						}
					}

					if (isStartAlsoEnd) {
						//System.out.println("STARTING TAG* - Adding to buff: " + tempBeginTag);
						indentBuff.append(tempBeginTag.trim());

						elementInstance.foundEndingTag = true;

						tempString = TAG_END_LEFT_BRACKET + xmlElement.elementName.trim() + TAG_RIGHT_BRACKET;
						//System.out.println("STARTING TAG* - Adding to buff: " + tempString);
						indentBuff.append(tempString.trim());
						//buff.append(TAG_LINE_FEED);
						buffList.add(indentBuff.toString());

						// TODO get rid of the "+ 1" to see if this helps
						nextTagBeginPos = tagEndPos + 1;

					} else {

						//System.out.println("STARTING TAG - Adding to buff: " + tempBeginTag);
						indentBuff.append(tempBeginTag.trim());

						// then check to see if this element contains other elements
						tempEndTag = TAG_END_LEFT_BRACKET + xmlElement.elementName.trim() + TAG_RIGHT_BRACKET;

						//System.out.println("STARTING TAG - tempEndTag: " + tempEndTag);
						//System.out.println("STARTING TAG - tagEndPos + 1: " + (tagEndPos + 1));


						tempPos = xml.indexOf(tempEndTag, (tagEndPos + 1));

						tempString = xml.substring((tagEndPos + 1), tempPos);
						//System.out.println("STARTING TAG - tempString: " + tempString);
						if (tempString.indexOf(TAG_END_LEFT_BRACKET) > -1 || tempString.indexOf(TAG_END_RIGHT_BRACKET) > -1) {
							//System.out.println("Tag " + xmlElement.elementName + " has nested elements");
							// the current element has other elements
							elementInstance.foundEndingTag = false;
							// increase the indent here
							currentDepth++;
							//buff.append(TAG_LINE_FEED);
							buffList.add(indentBuff.toString());
							nextTagBeginPos = elementInstance.startTagEndPos;

						} else {
							//System.out.println("Tag " + xmlElement.elementName + " has no nested elements");

							// the current element has other no other elements
							elementInstance.foundEndingTag = true;
							tempString = xml.substring(elementInstance.startTagEndPos, tempPos + xmlElement.elementName.length() + 3);
							//System.out.println("STARTING TAG - Adding to buff: " + tempString);
							indentBuff.append(tempString.trim());
							buffList.add(indentBuff.toString());
							//buff.append(TAG_LINE_FEED);

							// here we are trying to find the index where the
							// starting tag ends inside the xml string, this
							// will be the index we start searching from when
							// we loop next
							nextTagBeginPos = xml.indexOf(tempEndTag, tempPos - 1) + tempEndTag.length();
						}

					}


					beginPos = nextTagBeginPos;
					xmlElement.instances.add(elementInstance);


				}
			}

		} catch (Throwable ex) {
			ex.printStackTrace();
			//System.out.println(buff.toString());
		}


		return buffList;

	}

	public static TreeMap<Integer, String> getFileAsNumberedLines(String filePath) throws IOException {
		
		TreeMap<Integer, String> resultMap = new TreeMap<Integer, String>();
		if (filePath == null) return resultMap;
		
		filePath = FileUtils.convertFileSystemPath(filePath);
		
		File file = null;
		FileReader fr = null;
		LineNumberReader lnr = null;

		try {

			file = new File(filePath);
			fr = new FileReader(file);           
			lnr = new LineNumberReader(fr);

			String line = "";       
			int cntr = 0;
			
			while ((line = lnr.readLine()) != null) {
				cntr++;
				resultMap.put(new Integer(cntr), line);
			}
			
		} finally {
			if (fr != null) {
				fr.close();
			}
			if (lnr != null) {
				lnr.close();
			}

		}
		
		return resultMap;
	}
	
	public static String[] getFileAsArrayOfLines(String filePath) throws IOException {
		
		String[] result = new String[0];
		
		if (filePath == null) return result;
		
		filePath = FileUtils.convertFileSystemPath(filePath);
		
		File file = null;
		FileReader fr = null;
		LineNumberReader lnr = null;

		try {

			file = new File(filePath);
			fr = new FileReader(file);           
			lnr = new LineNumberReader(fr);
			
			String line = "";
			int cntr = 0;
			while ((line = lnr.readLine()) != null) {

				cntr++;
			
			}

			if (fr != null) {
				fr.close();
			}
			if (lnr != null) {
				lnr.close();
			}
			
			result = new String[cntr];			
			
			fr = new FileReader(file);           
			lnr = new LineNumberReader(fr);
			line = "";       
			
			cntr = 0;
			while ((line = lnr.readLine()) != null) {
				
				result[cntr] = line;
				cntr++;
				
			}
			
		} finally {

			if (fr != null) {
				fr.close();
			}
			if (lnr != null) {
				lnr.close();
			}

		}
		
		return result;
	}
	
	/**
	 * Returns a file as a String, the file path passed should adhere to one
	 * of the following formats in regard to line separators:
	 *
	 * D:\_data\stuff\myfile.txt
	 * D:/_data/stuff/myfile.txt
	 * D:\\_data\\stuff\\myfile.txt
	 * D://_data//stuff//myfile.txt
	 * \data\stuff\myfile.txt
	 * /data/stuff/myfile.txt
	 * \\data\\stuff\\myfile.txt
	 * //data//stuff//myfile.txt
	 *
	 *
	 * @param filePath
	 * @return the contents of the file as a String
	 * @throws Exception
	 */
	public static String getFileAsString(String filePath) throws Exception {

		filePath = FileUtils.convertFileSystemPath(filePath);

		FileInputStream fis = null;
		File file = new File(filePath);
		byte[] data = null;
		try {
			fis = new FileInputStream(file);

			data = new byte[fis.available()];
			fis.read(data);

		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		return new String(data);
	}

	/**
	 * Creates a file as a String, the file path passed should adhere to one
	 * of the following formats in regard to line separators:
	 *
	 * D:\_data\stuff\myfile.txt
	 * D:/_data/stuff/myfile.txt
	 * D:\\_data\\stuff\\myfile.txt
	 * D://_data//stuff//myfile.txt
	 * \data\stuff\myfile.txt
	 * /data/stuff/myfile.txt
	 * \\data\\stuff\\myfile.txt
	 * //data//stuff//myfile.txt
	 *
	 * @param filePath
	 * @param fileContents
	 * @return
	 * @throws Exception
	 */
	public static void saveStringAsFile(String filePath, String fileContents)
			throws Exception {

		filePath = FileUtils.convertFileSystemPath(filePath);

		File file = new File(filePath);
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			stream.write(fileContents.getBytes());
			stream.flush();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

	}

	public static void saveStringInFile(File file, String fileContents) throws Exception {
		
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			stream.write(fileContents.getBytes());
			stream.flush();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}
	
	
	/**
	 * Checks to see if the String passed is null or a zero length string
	 *
	 * @param val
	 * @return true if the "val" is null or a zero length string
	 */
	public static boolean isEmpty(String val) {

		if (val != null) {
			val = val.trim();
			if (val.equals("")) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public static boolean isNumeric(String val) {
		if (val == null) return false;
		String tmpVal = val = val.trim();
		if (tmpVal.equals("")) return false;
		try {
			Long.parseLong(tmpVal);
		} catch (Exception ex) {
			return false;
		}
		return true;
	}
	public static boolean isAlphaNumeric(String val) {
		if (val == null) return false;
		String tmpVal = val = val.trim();
		if (tmpVal.equals("")) return false;
		try {
			tmpVal = tmpVal.toUpperCase();
			String tmpChr;
			for (int i = 0; i < tmpVal.length(); i++) {
				tmpChr = tmpVal.substring(i, i + 1);
				//System.out.println(tmpChr);
				if (!ALPHA_CHARS.contains(tmpChr) && !NUMERIC_CHARS.contains(tmpChr)) {
					//System.out.println("Returning false");
					return false;
				}
			}

		} catch (Exception ex) {
			//ex.printStackTrace();
			return false;
		}
		//System.out.println("Returning true");
		return true;

	}

	public static boolean isAlpha(String val) {
		if (val == null) return false;
		String tmpVal = val = val.trim();
		if (tmpVal.equals("")) return false;
		try {
			tmpVal = tmpVal.toUpperCase();
			String tmpChr;
			for (int i = 0; i < tmpVal.length(); i++) {
				tmpChr = tmpVal.substring(i, i + 1);
				//System.out.println(tmpChr);
				if (!ALPHA_CHARS.contains(tmpChr)) {
					//System.out.println("Returning false");
					return false;
				}
			}

		} catch (Exception ex) {
			//ex.printStackTrace();
			return false;
		}
		//System.out.println("Returning true");
		return true;

	}

	/**
	 * Replaces all instances of the "stringSought" found within the
	 * "stringToSearch" with an instance of the "replacementString". This method
	 * assumes checks have already been done to ensure all the variables passed
	 * in are not null and have a value.
	 *
	 * @param stringToSearch -
	 *            the string that will be analyzed to determine if the
	 *            stringSought is contained within
	 *
	 * @param stringSought -
	 *            the string that will be sought or searched for within the
	 *            stringToSearch
	 *
	 * @param replacementString -
	 *            the string that will replace every occurrence found of the
	 *            stringSought within the stringToSearch
	 *
	 * @return the modified string after the replacements are made
	 */
	public static String replaceAll(String stringToSearch, String stringSought,
			String replacementString) {

		StringBuffer buff = new StringBuffer();
		int prevPos = 0;
		int currPos = 0;
		while (true) {
			currPos = stringToSearch.indexOf(stringSought, prevPos);
			if (currPos == -1) {
				if (prevPos < stringToSearch.length()) {
					buff.append(stringToSearch.substring(prevPos,
							stringToSearch.length()));
					return buff.toString();
				} else {
					return buff.toString();
				}
			} else {
				if (currPos != 0) {
					if (currPos != stringToSearch.length()
							- stringSought.length()) {
						buff.append(stringToSearch.substring(prevPos, currPos));
						buff.append(replacementString);
					} else {
						buff.append(stringToSearch.substring(prevPos, currPos));
						buff.append(replacementString);
						return buff.toString();
					}
				} else {
					buff.append(replacementString);
				}
				prevPos = currPos + stringSought.length();
			}
		}
	}

	/**
	 * Replaces the first instance of the "stringSought" found within the
	 * "stringToSearch" with an instance of the "replacementString". This method
	 * assumes checks have already been done to ensure all the variables passed
	 * in are not null and have a value.
	 *
	 * @param stringToSearch -
	 *            the string that will be analyzed to determine if the
	 *            stringSought is contained within
	 *
	 * @param stringSought -
	 *            the string that will be sought or searched for within the
	 *            stringToSearch
	 *
	 * @param replacementString -
	 *            the string that will replace the first occurrence found of the
	 *            stringSought within the stringToSearch
	 *
	 * @return the modified string after the replacements are made
	 */
	public static String replaceFirst(String stringToSearch,
			String stringSought, String replacementString) {

		StringBuffer buff = new StringBuffer();
		int prevPos = 0;
		int currPos = 0;

		currPos = stringToSearch.indexOf(stringSought, prevPos);
		if (currPos == -1) {
			return stringToSearch;
		} else {
			if (currPos != 0) {
				if (currPos != stringToSearch.length() - stringSought.length()) {
					buff.append(stringToSearch.substring(prevPos, currPos));
					buff.append(replacementString);
					prevPos = currPos + stringSought.length();
					buff.append(stringToSearch.substring(prevPos));
					return buff.toString();

				} else {
					buff.append(stringToSearch.substring(prevPos, currPos));
					buff.append(replacementString);
					return buff.toString();
				}
			} else {
				buff.append(replacementString);
				prevPos = currPos + stringSought.length();
				buff.append(stringToSearch.substring(prevPos));
				return buff.toString();
			}

		}
	}

	/**
	 * Replaces one instance of the "stringSought" found within the "stringToSearch"
	 * with an instance of the "replacementString", starting the search as the index
	 * specified by the "startingIndex" parameter. This method assumes checks have
	 * already been done to ensure all the variables passed in are not null and have
	 * a value.
	 *
	 * @param stringToSearch -
	 *            the string that will be analyzed to determine if the
	 *            stringSought is contained within
	 *
	 * @param stringSought -
	 *            the string that will be sought or searched for within the
	 *            stringToSearch
	 *
	 * @param replacementString -
	 *            the string that will replace the first occurrence found of the
	 *            stringSought within the stringToSearch
	 *
	 * @param startingIndex -
	 *            the index in the stringToSearch where the search will start at
	 *
	 * @return the modified string after the replacements are made
	 */
	public static String replaceOnceStartingAt(String stringToSearch,
			String stringSought, String replacementString, int startingIndex) {

		StringBuffer buff = new StringBuffer();
		int prevPos = 0;
		int currPos = 0;

		currPos = stringToSearch.indexOf(stringSought, startingIndex);
		if (currPos == -1) {
			return stringToSearch;
		} else {
			if (currPos != 0) {
				if (currPos != stringToSearch.length() - stringSought.length()) {
					buff.append(stringToSearch.substring(prevPos, currPos));
					buff.append(replacementString);
					prevPos = currPos + stringSought.length();
					buff.append(stringToSearch.substring(prevPos));
					return buff.toString();

				} else {
					buff.append(stringToSearch.substring(prevPos, currPos));
					buff.append(replacementString);
					return buff.toString();
				}
			} else {
				buff.append(replacementString);
				prevPos = currPos + stringSought.length();
				buff.append(stringToSearch.substring(prevPos));
				return buff.toString();
			}

		}
	}

	/**
	 * Removes all instances of the "stringToRemove" found within the
	 * "stringToSearch" with an instance of the "replacementString". This method
	 * assumes checks have already been done to ensure all the variables passed
	 * in are not null and have a value.
	 *
	 * @param stringToSearch -
	 *            the string that will be analyzed to determine if the
	 *            "stringToRemove" is contained within
	 *
	 * @param stringToRemove -
	 *            the string that will be sought or searched for within the
	 *            "stringToSearch"
	 *
	 * @return the modified string after the instances of the "stringToRemove"
	 *         are removed
	 */
	public static String removeAll(String stringToSearch, String stringToRemove) {

		StringBuffer buff = new StringBuffer();
		int prevPos = 0;
		int currPos = 0;
		while (true) {
			currPos = stringToSearch.indexOf(stringToRemove, prevPos);
			if (currPos == -1) {
				if (prevPos < stringToSearch.length()) {
					buff.append(stringToSearch.substring(prevPos,
							stringToSearch.length()));
					return buff.toString();
				} else {
					return buff.toString();
				}
			} else {
				if (currPos != 0) {
					if (currPos != stringToSearch.length()
							- stringToRemove.length()) {
						buff.append(stringToSearch.substring(prevPos, currPos));
					} else {
						buff.append(stringToSearch.substring(prevPos, currPos));
						return buff.toString();
					}
				}
				prevPos = currPos + stringToRemove.length();
			}
		}
	}

	/**
	 * Finds all instances of the "delimiter" found within the "stringToSearch"
	 * and splits out the strings between. This method assumes checks have
	 * already been done to ensure all the variables passed in are not null and
	 * have a value. This method assumes literal strings and not regex.
	 *
	 * @param stringToSearch -
	 *            the string that contains the delimiter
	 *
	 * @param delimiter -
	 *            the string that will be sought or searched for within the
	 *            "stringToSearch"
	 *
	 * @return the string array after the instances of the "delimiter" are found
	 */
	public static String[] split(String stringToSearch, String delimiter) {

		List<String> strLst = new ArrayList<String>();
		int prevPos = 0;
		int currPos = 0;
		while (true) {
			currPos = stringToSearch.indexOf(delimiter, prevPos);
			if (currPos == -1) {
				if (prevPos < stringToSearch.length()) {
					strLst.add(stringToSearch.substring(prevPos, stringToSearch
							.length()));
					return strLst.toArray(new String[strLst.size()]);
				} else {
					return strLst.toArray(new String[strLst.size()]);
				}
			} else {
				if (currPos != 0) {
					if (currPos != stringToSearch.length() - delimiter.length()) {
						strLst.add(stringToSearch.substring(prevPos, currPos));
					} else {
						strLst.add(stringToSearch.substring(prevPos, currPos));
						return strLst.toArray(new String[strLst.size()]);
					}
				}
				prevPos = currPos + delimiter.length();
			}
		}
	}


// #############################################################################################

	/**
	 * Removes Invalid characters defined in invalidChars from a given string.
	 * e.g: removeInvalidCharacters("817.233-1222",new chars[] {'-','.'}) will
	 * return "8172331222".
	 *
	 * @param String
	 *            str String invalidChars
	 * @return
	 */
	public static String removeInvalidCharacters(String str, String invalidChars) {

		StringBuffer sBuf = new StringBuffer(str);

		for (int i = 0; i < sBuf.length(); i++) {
			if (invalidChars.indexOf(sBuf.charAt(i)) != -1) {
				sBuf.deleteCharAt(i--);
			}
		}
		return sBuf.toString();

	}

	public static String currentDateTimeToString() {
		SimpleDateFormat f = new SimpleDateFormat(FILE_STAMP_STRING_DATE_FORMAT);
		Calendar c = Calendar.getInstance();
		f.setCalendar(c);
		return f.format(c.getTime());
	}

   /**
    *
    * @param c
    *            instance of String to be converted to calendar representation
    * @param format
    *            format to convert the string to - must be a valid Java format
    * @return string representation of calendar instance in requested format
    */
   public static Calendar stringToDate(String calStr, String format) {

      if (calStr == null) {
         return null;
      }

      final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
      try {
         Date date =  dateFormat.parse(calStr);
         Calendar cal = Calendar.getInstance();
         cal.setTime(date);
         return cal;
      } catch (ParseException e) {
         return null;
      }
   }

   /**
   *
   * @param c
   *            instance of calendar to be converted to string representation
   * @param format
   *            format of the string - must be a valid Java format
   * @return string representation of calendar instance in requested format
   */
  public static String dateToString(Calendar c, String format) {

     if (c == null) {
        return null;
     }
     SimpleDateFormat f = new SimpleDateFormat(format);
     f.setCalendar(c);
     return f.format(c.getTime());
  }

	public static String longDateToString(long dateInMillis) {
		SimpleDateFormat f = new SimpleDateFormat(STRING_DATE_FORMAT);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(dateInMillis);
		f.setCalendar(c);
		return f.format(c.getTime());
	}

	public static String longDateToString(long dateInMillis, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(dateInMillis);
		f.setCalendar(c);
		return f.format(c.getTime());
	}

	/**
	 * Return an special escaped version of the
	 * given text string where XML special characters
	 * have been replaced with substitutions.
	 * For a null string we return null
	 *
	 * @param text
	 * @return
	 */
	public static String escapeConfig(String text) {
		if (text == null) {
			return null;
		}
		String retStr;
		// replace the left prefix of an ending tag
		retStr = replaceAll(text, "</", "~:LBS:~");
		retStr = replaceAll(retStr, "<", "~:LB:~");
		retStr = replaceAll(retStr, ">", "~:RB:~");
		return retStr;
	}

	/**
	 * Return an special unescaped version of the
	 * given text string where substitutions
	 * have been replaced with XML special characters.
	 * For a null string we return null
	 *
	 * @param text
	 * @return
	 */
	public static String unescapeConfig(String text) {
		if (text == null) {
			return null;
		}
		String retStr;
		// replace the left prefix of an ending tag
		retStr = replaceAll(text, "~:LBS:~", "</");
		retStr = replaceAll(retStr, "~:LB:~", "<");
		retStr = replaceAll(retStr, "~:RB:~", ">");
		return retStr;
	}

}

