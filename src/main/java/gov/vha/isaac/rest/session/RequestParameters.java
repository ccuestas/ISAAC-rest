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

package gov.vha.isaac.rest.session;

/**
 * 
 * {@link RequestParameters}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class RequestParameters {
	private RequestParameters() {}

	// CoordinatesToken
	public final static String coordToken = "coordToken";

	// Taxonomy Coordinate
	public final static String stated = "stated";
	public final static String statedDefault = "true";

	// Language Coordinate
	public static enum LanguageCoordinateParamNames {
		language,
		dialectPrefs,
		descriptionTypePrefs
	};
	public final static String language = LanguageCoordinateParamNames.language.name();
	public final static String languageDefault = "english";
	public final static String dialectPrefs = LanguageCoordinateParamNames.dialectPrefs.name();
	public final static String dialectPrefsDefault = "us,gb";
	public final static String descriptionTypePrefs = LanguageCoordinateParamNames.descriptionTypePrefs.name();
	public final static String descriptionTypePrefsDefault = "fsn,synonym";
	
	// Stamp Coordinate
	public static enum StampCoordinateParamNames {
		time,
		path,
		precedence,
		modules,
		allowedStates
	};
	public final static String time = StampCoordinateParamNames.time.name();
	public final static String timeDefault = "latest";
	public final static String path = StampCoordinateParamNames.path.name();
	public final static String pathDefault = "development";
	public final static String precedence = StampCoordinateParamNames.precedence.name();
	public final static String precedenceDefault = "path";
	public final static String modules = StampCoordinateParamNames.modules.name();
	public final static String modulesDefault = "";
	public final static String allowedStates = StampCoordinateParamNames.allowedStates.name();
	public final static String allowedStatesDefault = "active";

	// Logic Coordinate
	public static enum LogicCoordinateParamNames {
		logicStatedAssemblage,
		logicInferredAssemblage,
		descriptionLogicProfile,
		classifier
	};
	public final static String logicStatedAssemblage = LogicCoordinateParamNames.logicStatedAssemblage.name();
	public final static String logicStatedAssemblageDefault = "1f201994-960e-11e5-8994-feff819cdc9f";
	public final static String logicInferredAssemblage = LogicCoordinateParamNames.logicInferredAssemblage.name();
	public final static String logicInferredAssemblageDefault = "1f20182c-960e-11e5-8994-feff819cdc9f";
	public final static String descriptionLogicProfile = LogicCoordinateParamNames.descriptionLogicProfile.name();
	public final static String descriptionLogicProfileDefault = "1f201e12-960e-11e5-8994-feff819cdc9f";
	public final static String classifier = LogicCoordinateParamNames.classifier.name();
	public final static String classifierDefault = "1f201fac-960e-11e5-8994-feff819cdc9f";

	public final static String id = "id";
	public final static String nid = "nid";
	public final static String expand = "expand";

	public final static String pageNum = "pageNum";
	public final static String pageNumDefault = "1";

	public final static String maxPageSize = "maxPageSize";
	public final static String maxPageSizeDefault = "10";

	public final static String assemblage = "assemblage";
	public final static String includeDescriptions = "includeDescriptions";
	public final static String includeAttributes = "includeAttributes";
	public final static String includeAttributesDefault = "true";

	public final static String query = "query";
	public final static String treatAsString = "treatAsString";
	public final static String descriptionType = "descriptionType";
	public final static String extendedDescriptionTypeId = "extendedDescriptionTypeId";
	public final static String dynamicSememeColumns = "dynamicSememeColumns";
	public final static String sememeAssemblageId = "sememeAssemblageId";

}
