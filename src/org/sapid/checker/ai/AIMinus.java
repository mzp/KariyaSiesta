/*
 * Copyright(c) 2009 Nagoya University
 *  All Rights Reserved
 */
package org.sapid.checker.ai;

/**
 * ������ɽ����ݥ��饹�����󥹥ȥ饯���Υ��󥿡��ե�������������롥
 * @author hirumuta
 *
 */
public abstract class AIMinus implements NonterminalExpression {
	
	public AIMinus(Value left_value,Value right_value) {
	}
	
	public abstract Value interpret(AIEnvironment env);

}