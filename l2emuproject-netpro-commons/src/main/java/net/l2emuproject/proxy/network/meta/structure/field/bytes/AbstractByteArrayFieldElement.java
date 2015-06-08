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
package net.l2emuproject.proxy.network.meta.structure.field.bytes;

import static net.l2emuproject.proxy.network.meta.structure.field.FieldValueReadOption.APPLY_MODIFICATIONS;
import static net.l2emuproject.proxy.network.meta.structure.field.FieldValueReadOption.COMPUTE_INTERPRETATION;

import java.nio.BufferUnderflowException;
import java.util.Map;
import java.util.Set;

import net.l2emuproject.network.mmocore.MMOBuffer;
import net.l2emuproject.proxy.network.meta.container.MetaclassRegistry;
import net.l2emuproject.proxy.network.meta.exception.InvalidFieldValueInterpreterException;
import net.l2emuproject.proxy.network.meta.exception.InvalidFieldValueModifierException;
import net.l2emuproject.proxy.network.meta.interpreter.ByteArrayInterpreter;
import net.l2emuproject.proxy.network.meta.interpreter.ContextualFieldValueInterpreter;
import net.l2emuproject.proxy.network.meta.modifier.ByteArrayModifier;
import net.l2emuproject.proxy.network.meta.structure.FieldElement;
import net.l2emuproject.proxy.network.meta.structure.field.FieldValueReadOption;
import net.l2emuproject.proxy.network.meta.structure.field.InterpreterContext;

/**
 * A class that represents a byte array field.
 * 
 * @author _dev_
 */
public abstract class AbstractByteArrayFieldElement extends FieldElement<ByteArrayFieldValue>
{
	/**
	 * Constructs this field element.
	 * 
	 * @param id field ID
	 * @param alias field description
	 * @param optional whether this field may be excluded from packet
	 * @param fieldAliases field IDs for scripts
	 * @param valueModifier associated value modifier
	 * @param valueInterpreter associated value interpreter
	 */
	protected AbstractByteArrayFieldElement(String id, String alias, boolean optional, Set<String> fieldAliases,
			String valueModifier, String valueInterpreter)
	{
		super(id, alias, optional, fieldAliases, valueModifier, valueInterpreter);
	}
	
	/**
	 * Reads bytes from the given buffer into an array.
	 * 
	 * @param buf buffer
	 * @param args optional arguments
	 * @return byte array
	 * @throws BufferUnderflowException if the value cannot be read
	 */
	protected abstract byte[] readValue(MMOBuffer buf, Object... args) throws BufferUnderflowException;
	
	@Override
	public ByteArrayFieldValue readValue(MMOBuffer buf, Map<FieldValueReadOption, ?> options, Object... args) throws BufferUnderflowException, InvalidFieldValueInterpreterException, InvalidFieldValueModifierException
	{
		final byte[] value;
		try
		{
			value = readValue(buf, args);
		}
		catch (BufferUnderflowException e)
		{
			if (isOptional())
				return null;
			throw e;
		}
		
		final byte[] raw = buf.lastRead();
		final String vm = options.containsKey(APPLY_MODIFICATIONS) ? getValueModifier() : null, vi = options.containsKey(COMPUTE_INTERPRETATION) ? getValueInterpreter() : null;
		
		final MetaclassRegistry mcr = MetaclassRegistry.getInstance();
		final byte[] modifiedValue = vm != null ? mcr.getModifier(vm, ByteArrayModifier.class).apply(value) : value;
		final Object interpreted;
		if (vi != null)
		{
			final InterpreterContext ctx = (InterpreterContext)options.get(COMPUTE_INTERPRETATION);
			final ByteArrayInterpreter interpreter = mcr.getInterpreter(vi, ByteArrayInterpreter.class);
			if (interpreter instanceof ContextualFieldValueInterpreter)
				((ContextualFieldValueInterpreter)interpreter).reviewContext(ctx.getWireframe());
			interpreted = interpreter.getInterpretation(modifiedValue, ctx.getEntityContext());
		}
		else
			interpreted = modifiedValue;
		return new ByteArrayFieldValue(raw, interpreted, modifiedValue);
	}
}
