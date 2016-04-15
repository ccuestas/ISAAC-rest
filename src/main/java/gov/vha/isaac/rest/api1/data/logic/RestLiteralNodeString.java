/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.vha.isaac.rest.api1.data.logic;

import javax.xml.bind.annotation.XmlElement;
import gov.vha.isaac.ochre.model.logic.node.LiteralNodeString;

/**
 * 
 * {@link RestLiteralNodeString}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 * RestLiteralNodeString is a logic node containing only a String literal value
 * 
 * Each RestLiteralNodeString instance has a RestNodeSemanticType/NodeSemantic == NodeSemantic.LITERAL_STRING
 * 
 * A RestLiteralNodeString may not have any child logic nodes.
 */
public class RestLiteralNodeString extends RestLogicNode {

	/**
	 * RestLiteralNodeString contains a literal String value, literalValue
	 */
	@XmlElement
	String literalValue;

	protected RestLiteralNodeString() {
		// For JAXB
	}
	/**
	 * @param literalNodeString
	 */
	public RestLiteralNodeString(LiteralNodeString literalNodeString) {
		super(literalNodeString);
		literalValue = literalNodeString.getLiteralValue();
	}
}
