/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2023 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.aem.graphql.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a GraphQl query.
 */
public class GraphQlQuery {

	public static class SortBy {
		private final String field;
		private final SortingOrder sortByOrder;

		public SortBy(String field, SortingOrder sortByOrder) {
			this.field = field;
			this.sortByOrder = sortByOrder;
		}

		public SortBy(String fieldWithSortingOrder) {
			String[] fieldAndSortingOrder = fieldWithSortingOrder.split(" ", 2);
			this.field = fieldAndSortingOrder[0];
			this.sortByOrder = fieldAndSortingOrder.length == 2
					? SortingOrder.valueOf(fieldAndSortingOrder[1].toUpperCase())
					: SortingOrder.ASC;
		}
	}

	public enum SortingOrder {
		ASC, DESC
	}

	private AEMHeadlessClient client;

	private String contentFragementModelName;
	private List<String> fields = new ArrayList<>();
	private List<SortBy> sortByList = new ArrayList<>();

	/**
	 * Builder that allows to configure all available options of the
	 * {@code AEMHeadlessQuery}
	 * 
	 * @return builder
	 * 
	 */
	public static @NotNull GraphQlQueryBuilder builder() {
		return new GraphQlQueryBuilder();
	}

	GraphQlQuery() {
		// used by builder only
	}

	void setAEMHeadlessClient(AEMHeadlessClient client) {
		this.client = client;
	}

	void setContentFragementModelName(String contentFragementModelName) {
		this.contentFragementModelName = contentFragementModelName;
	}

	void addFields(String... fields) {
		this.fields.addAll(Arrays.asList(fields));
	}

	void addSorting(SortBy sortBy) {
		sortByList.add(sortBy);
	}

	private String getSortParamValue() {
		return sortByList.stream().map(item -> item.field + " " + item.sortByOrder).collect(Collectors.joining(", "));
	}

	String generateQuery(boolean useCursorPagination, Map<String, Object> topLevelQueryArguments) {
		StringBuilder buf = new StringBuilder();

		buf.append("query { ");
		buf.append(contentFragementModelName);
		buf.append(useCursorPagination ? "Paginated" : "List");

		Map<String, Object> effectiveTopLevelQueryArguments = new HashMap<>();

		if (!sortByList.isEmpty()) {
			effectiveTopLevelQueryArguments.put("sort", getSortParamValue());
		}
		if (topLevelQueryArguments != null) {
			effectiveTopLevelQueryArguments.putAll(topLevelQueryArguments);
		}

		if (!effectiveTopLevelQueryArguments.isEmpty()) {
			buf.append("(");
			boolean isFirst = true;
			for (String key : effectiveTopLevelQueryArguments.keySet()) {
				if (isFirst) {
					isFirst = false;
				} else {
					buf.append(", ");
				}
				buf.append(key + ": ");
				Object val = effectiveTopLevelQueryArguments.get(key);
				if (val instanceof Number) {
					buf.append(val);
				} else {
					buf.append("\"" + val + "\"");
				}
			}
			buf.append(")");
		}
		buf.append(" {\n");

		String fieldsStr = "      " + String.join("\n      ", fields) + "\n";
		if (useCursorPagination) {
			buf.append("    edges { node {\n" + fieldsStr + "  }}\n  pageInfo { hasNextPage endCursor }\n");
		} else {
			buf.append("    items {\n" + fieldsStr + " }\n");
		}

		buf.append("  }\n");
		buf.append("}\n");
		return buf.toString();
	}

	public interface QueryCursor {
		public GraphQlResponse next(long number);

		public boolean hasMore();
	}

	private class CursorImpl implements QueryCursor {

		private boolean hasMore = true;
		private String endCursor = null;

		@Override
		public GraphQlResponse next(long number) {

			Map<String, Object> params = new HashMap<>();
			params.put("first", number);
			if (endCursor != null) {
				params.put("after", endCursor);
			}
			String query = generateQuery(true, params);

			GraphQlResponse response = client.runQuery(query);
			if (!response.hasErrors()) {
				JsonNode pageInfoResult = response.getData().get(contentFragementModelName + "Paginated")
						.get("pageInfo");
				hasMore = pageInfoResult.get("hasNextPage").asBoolean();
				endCursor = pageInfoResult.get("endCursor").asText();
			}

			return response;
		}

		@Override
		public boolean hasMore() {
			return hasMore;
		}
	}

	public QueryCursor getCursor() {
		return new CursorImpl();
	}

}
