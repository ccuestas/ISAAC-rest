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

package gov.vha.isaac.rest.api1.data.enumerations;

import javax.xml.bind.annotation.XmlRootElement;

import gov.vha.isaac.ochre.model.logic.ConcreteDomainOperators;

/**
 * 
 * {@link RestConcreteDomainOperators}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@XmlRootElement
public class RestConcreteDomainOperators extends Enumeration
{
	protected RestConcreteDomainOperators()
	{
		//for jaxb
	}
	
	public RestConcreteDomainOperators(ConcreteDomainOperators st)
	{
		super(st.toString(), st.ordinal());
	}
	
	public static RestConcreteDomainOperators[] getAll()
	{
		RestConcreteDomainOperators[] result = new RestConcreteDomainOperators[ConcreteDomainOperators.values().length];
		for (int i = 0; i < ConcreteDomainOperators.values().length; i++)
		{
			result[i] = new RestConcreteDomainOperators(ConcreteDomainOperators.values()[i]);
		}
		return result;
	}
}