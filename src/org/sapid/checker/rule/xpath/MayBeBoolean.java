package org.sapid.checker.rule.xpath;

import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

import org.sapid.checker.cx.wrapper.CExpressionElement;
import org.sapid.checker.cx.wrapper.type.TagType;
import org.sapid.checker.cx.wrapper.type.Type;
import org.sapid.checker.cx.wrapper.type.TypedefType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MayBeBoolean implements XPathFunction {
	
	private static MayBeBoolean instance;
	
	
	private MayBeBoolean() {
	}
	
	
	public static MayBeBoolean getInstance() {
		if (instance == null) {
			instance = new MayBeBoolean();
		}
		
		return instance;
	}
	

	@Override
	@SuppressWarnings("unchecked")
	public Boolean evaluate(List args) throws XPathFunctionException {
		if (args == null) {
			throw new XPathFunctionException("Null argument");
		}
		
		if (args.size() != 1) {
			throw new XPathFunctionException("Wrong number of arguments:" + args.size());
		}
		
		Object argument = args.get(0);
		if (! (argument instanceof NodeList)) {
			throw new XPathFunctionException("Wrong type of argument:" + argument.getClass());
		}
		
		NodeList argumentNodeList = (NodeList) argument;
		if (argumentNodeList.getLength() != 1) {
			throw new XPathFunctionException("Wrong number of argument nodes:" + argumentNodeList.getLength());
		}
		
		Node argumentNode = argumentNodeList.item(0);
		if (argumentNode.getNodeType() != Node.ELEMENT_NODE) {
			throw new XPathFunctionException("Wrong type of argument node:" + argumentNode.getNodeType());
		}
		
		Element argumentElement = (Element) argumentNode;
		if (! argumentElement.getNodeName().equals("Expr")) {
			throw new XPathFunctionException("Wrong name of argument element:" + argumentElement.getNodeName());
		}
		
		CExpressionElement expression = new CExpressionElement(argumentElement);
		CExpressionElement.Sort sort = expression.getSortEnum();
		
		if (sort == CExpressionElement.Sort.COMPARISON || sort == CExpressionElement.Sort.LOGICAL) {
			return true;
		}
		
		Type type = expression.getTypeInfo();
		if (type == null) {
			return false;
		} else if (type.getSort() == Type.Sort.TYPEDEF) {
			type = ((TypedefType) type).getTrueTypeRecursively();
		}
		
		if (type.getSort() == Type.Sort.ENUM) {
			TagType enumType = (TagType) type;
			if (enumType.getMemberTypes().length == 2) {
				return true;
			}
		}
		
		return false;
	}

}
