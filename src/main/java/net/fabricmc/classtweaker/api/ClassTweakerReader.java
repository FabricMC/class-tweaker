/*
 * Copyright (c) 2020 FabricMC
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

package net.fabricmc.classtweaker.api;

import java.io.BufferedReader;
import java.io.IOException;

import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.classtweaker.reader.ClassTweakerReaderImpl;

public interface ClassTweakerReader {
	static ClassTweakerReader create(ClassTweakerVisitor visitor) {
		return new ClassTweakerReaderImpl(visitor);
	}

	void read(byte[] content, String id);

	void read(byte[] content, String currentNamespace, String id);

	void read(BufferedReader reader, String id) throws IOException;

	void read(BufferedReader reader, String currentNamespace, String id) throws IOException;

	static int readVersion(byte[] content) {
		return ClassTweakerReaderImpl.readVersion(content);
	}

	static int readVersion(BufferedReader bufferedReader) throws IOException {
		return ClassTweakerReaderImpl.readVersion(bufferedReader);
	}

	static Header readHeader(byte[] content) {
		return ClassTweakerReaderImpl.readHeader(content);
	}

	static Header readHeader(BufferedReader reader) throws IOException {
		return ClassTweakerReaderImpl.readHeader(reader);
	}

	interface Header {
		int getVersion();
		String getNamespace();
	}
}
