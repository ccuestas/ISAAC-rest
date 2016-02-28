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
import javax.xml.bind.annotation.XmlRootElement;

import gov.vha.isaac.ochre.model.logic.node.external.FeatureNodeWithUuids;
import gov.vha.isaac.rest.api1.data.enumerations.RestConcreteDomainOperators;

/**
 * 
 * {@link RestFeatureNodeWithUuids}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@XmlRootElement
public class RestFeatureNodeWithUuids extends RestTypedNodeWithUuids {

	@XmlElement
	RestConcreteDomainOperators operator;
	
	protected RestFeatureNodeWithUuids() {
		// For JAXB
	}
	/**
	 * @param featureNodeWithUuids
	 */
	public RestFeatureNodeWithUuids(FeatureNodeWithUuids featureNodeWithUuids) {
		super(featureNodeWithUuids);
		operator = new RestConcreteDomainOperators(featureNodeWithUuids.getOperator());
	}

	@Override
    public String toString(String nodeIdSuffix) {
        return "Feature[" + nodeIndex + nodeIdSuffix +"] "
                + operator
                + ", units:" //+ Get.conceptDescriptionText(unitsConceptSequence)
                + super.toString(nodeIdSuffix);
    }
}
