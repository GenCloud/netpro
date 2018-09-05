/*
 * Copyright 2011-2015 L2EMU UNIQUE
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
package net.l2emuproject.proxy.network.meta.interpreter;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.l2emuproject.network.protocol.IProtocolVersion;
import net.l2emuproject.proxy.state.entity.context.ICacheServerID;
import net.l2emuproject.util.logging.L2Logger;

/**
 * Interprets the given byte/word/dword as a key of the internal interpretation map.
 * 
 * @author savormix
 */
public abstract class IntegerIdTranslator implements IntegerTranslator
{
	private final Map<IProtocolVersion, Map<Long, ?>> _id2InterpretationWrapper;
	private final Object _unknownKeyInterpretation;
	
	/**
	 * Constructs this interpreter.
	 * 
	 * @param id2InterpretationWrapper known ID interpretations
	 * @param unknownKeyInterpretation unknown ID interpretation or {@code null}
	 */
	protected IntegerIdTranslator(Map<IProtocolVersion, Map<Long, ?>> id2InterpretationWrapper, Object unknownKeyInterpretation)
	{
		_id2InterpretationWrapper = id2InterpretationWrapper;
		_unknownKeyInterpretation = unknownKeyInterpretation;
	}
	
	/**
	 * Constructs this interpreter.
	 * 
	 * @param id2InterpretationWrapper known ID interpretations
	 */
	protected IntegerIdTranslator(Map<IProtocolVersion, Map<Long, ?>> id2InterpretationWrapper)
	{
		this(id2InterpretationWrapper, null);
	}
	
	@Override
	public Boolean isKnown(long value, IProtocolVersion protocol, ICacheServerID entityCacheContext)
	{
		return _id2InterpretationWrapper.getOrDefault(protocol, _id2InterpretationWrapper.getOrDefault(null, Collections.emptyMap())).containsKey(value);
	}
	
	@Override
	public Object translate(long value, IProtocolVersion protocol, ICacheServerID entityCacheContext)
	{
		Object result = _id2InterpretationWrapper.getOrDefault(protocol, _id2InterpretationWrapper.getOrDefault(null, Collections.emptyMap())).get(value);
		if (result == null)
			result = _unknownKeyInterpretation;
		if (result == null)
			result = value;
		return result;
	}
	
	/**
	 * Loads ID interpretations from a bundled resource file.
	 * 
	 * @param clazz sibling class
	 * @param resourceName file name
	 * @return integer ID interpretations
	 */
	protected static final Map<Long, String> loadFromClasspathResource(Class<?> clazz, String resourceName)
	{
		try (final BufferedReader br = new BufferedReader(new InputStreamReader(clazz.getResourceAsStream(resourceName), UTF_8)))
		{
			final Map<Long, String> mapping = new HashMap<>();
			for (String line; (line = br.readLine()) != null;)
			{
				final int idx = line.indexOf('\t');
				if (idx == -1)
					continue;
				
				final Long id = Long.valueOf(line.substring(0, idx));
				final String name = line.substring(idx + 1);
				mapping.put(id, name.intern());
			}
			return mapping;
		}
		catch (final IOException e)
		{
			L2Logger.getLogger(clazz).error("Could not load integer ID interpretations from " + resourceName, e);
			return Collections.emptyMap();
		}
	}
}
