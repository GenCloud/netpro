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
package interpreter.auth;

import java.nio.charset.StandardCharsets;

import net.l2emuproject.network.protocol.IProtocolVersion;
import net.l2emuproject.proxy.network.meta.interpreter.ByteArrayTranslator;
import net.l2emuproject.proxy.script.interpreter.ScriptedFieldValueInterpreter;
import net.l2emuproject.proxy.state.entity.context.ICacheServerID;

/**
 * Interprets the given 40 byte array as a custom server name.
 * 
 * @author _dev_
 */
public class CustomServerName extends ScriptedFieldValueInterpreter implements ByteArrayTranslator
{
	@Override
	public Object translate(byte[] value, IProtocolVersion protocol, ICacheServerID entityCacheContext)
	{
		return new String(value, StandardCharsets.UTF_16LE).trim();
	}
}
