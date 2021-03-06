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

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import gov.vha.isaac.ochre.model.logic.node.AndNode;
import gov.vha.isaac.ochre.model.logic.node.DisjointWithNode;
import gov.vha.isaac.ochre.model.logic.node.NecessarySetNode;
import gov.vha.isaac.ochre.model.logic.node.OrNode;
import gov.vha.isaac.ochre.model.logic.node.RootNode;
import gov.vha.isaac.ochre.model.logic.node.SufficientSetNode;

/**
 * 
 * {@link RestUntypedConnectorNode}
 *
 * RestUntypedConnectorNode is the class representing untyped connector nodes.
 * 
 * A RestUntypedConnectorNode instance RestNodeSemanticType/NodeSemantic value varies
 * according to its type: DEFINITION_ROOT, AND, OR, DISJOINT_WITH, NECESSARY_SET or SUFFICIENT_SET
 * 
 * A RestUntypedConnectorNode must have 1 or more child nodes.
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class RestUntypedConnectorNode extends RestLogicNode {
	protected RestUntypedConnectorNode() {
		// For JAXB
	}

	/**
	 * @param rootNode
	 */
	public RestUntypedConnectorNode(RootNode rootNode) {
		super(rootNode);
	}
	/**
	 * @param andNode
	 */
	public RestUntypedConnectorNode(AndNode andNode) {
		super(andNode);
	}
	/**
	 * @param orNode
	 */
	public RestUntypedConnectorNode(OrNode orNode) {
		super(orNode);
	}
	/**
	 * @param disjointWithNode
	 */
	public RestUntypedConnectorNode(DisjointWithNode disjointWithNode) {
		super(disjointWithNode);
	}
	/**
	 * @param necessarySetNode
	 */
	public RestUntypedConnectorNode(NecessarySetNode necessarySetNode) {
		super(necessarySetNode);
	}
	/**
	 * @param sufficientSetNode
	 */
	public RestUntypedConnectorNode(SufficientSetNode sufficientSetNode) {
		super(sufficientSetNode);
	}
}