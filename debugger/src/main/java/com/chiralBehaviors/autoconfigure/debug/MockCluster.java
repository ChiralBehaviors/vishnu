/** (C) Copyright 2014 Chiral Behaviors, All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package com.chiralBehaviors.autoconfigure.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import com.chiralBehaviors.autoconfigure.Service;

public class MockCluster {
	private final List<Map<String, String>> cluster = new ArrayList<>();

	public MockCluster(List<Map<String, String>> members) {
		this.cluster.addAll(members);
	}

	public int getCardinality() {
		return cluster.size();
	}

	public Service getFirst() {
		throw new STNoSuchPropertyException(null, this, "first");
	}

	public List<Map<String, String>> getMembers() {
		return cluster;
	}
}
