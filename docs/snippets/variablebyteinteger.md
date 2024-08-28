## How the variable bytes integer works

[Encoding:](https://github.com/eclipse/paho.mqtt.java/blob/9c742c1d83e71132452e08d325e5a90d1631c302/org.eclipse.paho.mqttv5.client/src/main/java/org/eclipse/paho/mqttv5/common/packet/MqttWireMessage.java#L277)

<pre>
    public static byte[] encodeVariableByteInteger(int number) {
		int numBytes = 0;
		long no = number;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Encode the remaining length fields in the four bytes
		do {
			byte digit = (byte) (no % 128);
			no = no / 128;
			if (no > 0) {
				digit |= 0x80;
			}
			baos.write(digit);
			numBytes++;
		} while ((no > 0) && (numBytes < 4));
		return baos.toByteArray();
	}
</pre>

[Decoding:](https://github.com/eclipse/paho.mqtt.java/blob/master/org.eclipse.paho.mqttv5.client/src/main/java/org/eclipse/paho/mqttv5/common/packet/MqttDataTypes.java#L213)

<pre>
	public static VariableByteInteger readVariableByteInteger(DataInputStream in) throws IOException {
		byte digit;
		int value = 0;
		int multiplier = 1;
		int count = 0;

		do {
			digit = in.readByte();
			count++;
			value += ((digit & 0x7F) * multiplier);
			multiplier *= 128;
		} while ((digit & 0x80) != 0);

		if (value < 0 || value > VARIABLE_BYTE_INT_MAX) {
			throw new IOException("This property must be a number between 0 and " + VARIABLE_BYTE_INT_MAX
					+ ". Read value was: " + value);
		}

		return new VariableByteInteger(value, count);

	}
</pre>