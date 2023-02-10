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

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

import com.adobe.aem.graphql.client.GraphQlQuery.PaginationType;

/**
 * Builds a GraphQL query.
 */
public class GraphQlQueryBuilder {
	

	private final GraphQlQuery headlessQuery;
	private boolean sealed = false;

	/**
	 * Use {@link GraphQlQuery#builder()} to create a builder.
	 */
	GraphQlQueryBuilder() {
		headlessQuery = new GraphQlQuery();
	}

	public GraphQlQueryBuilder contentFragmentModelName(@NotNull String name) {
		headlessQuery.setContentFragementModelName(name);
		return this;
	}

	
	public GraphQlQueryBuilder fields(@NotNull String... fields) {
		headlessQuery.addFields(fields);
		return this;
	}

	public GraphQlQueryBuilder field(@NotNull String field) {
		headlessQuery.addFields(field);
		return this;
	}
	
	public GraphQlQueryBuilder paginated() {
		headlessQuery.setPaginationType(PaginationType.CURSOR);
		return this;
	}

	public GraphQlQueryBuilder paginated(PaginationType type) {
		headlessQuery.setPaginationType(type);
		return this;
	}
	
	public GraphQlQueryBuilder sortBy(@NotNull String sortByField, @NotNull GraphQlQuery.SortingOrder order) {
		headlessQuery.addSorting(new GraphQlQuery.SortBy(sortByField, order));
		return this;
	}

	public GraphQlQueryBuilder sortBy(@NotNull String... sortByFieldWithOrderClauses) {
		Arrays.asList(sortByFieldWithOrderClauses).stream().forEach(
				sortByFieldWithOrder -> headlessQuery.addSorting(new GraphQlQuery.SortBy(sortByFieldWithOrder)));
		return this;
	}

	public @NotNull GraphQlQuery build() {
		assertNotSealed();
		sealed = true;
		return headlessQuery;
	}
	
	public @NotNull String generate() {
		return build().generateQuery();
	}
	
	private void assertNotSealed() {
		if (sealed) {
			throw new IllegalStateException("Builder can only be used to create one instance of AEMHeadlessClient");
		}
	}
}
