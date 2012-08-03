// ========================================================================
// Copyright 2011-2012 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
//
//     The Eclipse Public License is available at
//     http://www.eclipse.org/legal/epl-v10.html
//
//     The Apache License v2.0 is available at
//     http://www.opensource.org/licenses/apache2.0.php
//
// You may elect to redistribute this code under either of these licenses.
//========================================================================
package org.eclipse.jetty.websocket.server.ab;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.toolchain.test.AdvancedRunner;
import org.eclipse.jetty.toolchain.test.annotation.Slow;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.protocol.CloseInfo;
import org.eclipse.jetty.websocket.protocol.OpCode;
import org.eclipse.jetty.websocket.protocol.WebSocketFrame;
import org.eclipse.jetty.websocket.server.helper.Hex;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UTF-8 Tests
 */
@RunWith(AdvancedRunner.class)
public class TestABCase6 extends AbstractABCase
{
    private void assertBadTextPayload(byte[] invalid) throws Exception
    {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new WebSocketFrame(OpCode.TEXT).setPayload(invalid));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.BAD_PAYLOAD).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    private void assertEchoTextMessage(byte[] msg) throws Exception
    {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new WebSocketFrame(OpCode.TEXT).setPayload(msg));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new WebSocketFrame(OpCode.TEXT).setPayload(msg));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Split a message byte array into a series of fragments (frames + continuations) of 1 byte message contents each.
     */
    protected void fragmentText(List<WebSocketFrame> frames, byte msg[])
    {
        int len = msg.length;
        byte opcode = OpCode.TEXT;
        byte mini[];
        for (int i = 0; i < len; i++)
        {
            WebSocketFrame frame = new WebSocketFrame(opcode);
            mini = new byte[1];
            mini[0] = msg[i];
            frame.setPayload(mini);
            boolean isLast = (i >= (len - 1));
            frame.setFin(isLast);
            frames.add(frame);
            opcode = OpCode.CONTINUATION;
        }
    }

    /**
     * text message, 1 frame, 0 length
     */
    @Test
    public void testCase6_1_1() throws Exception
    {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text());
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text());
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * text message, 0 length, 3 fragments
     */
    @Test
    public void testCase6_1_2() throws Exception
    {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new WebSocketFrame(OpCode.TEXT).setFin(false));
        send.add(new WebSocketFrame(OpCode.CONTINUATION).setFin(false));
        send.add(new WebSocketFrame(OpCode.CONTINUATION).setFin(true));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text());
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * text message, small length, 3 fragments (only middle frame has payload)
     */
    @Test
    public void testCase6_1_3() throws Exception
    {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new WebSocketFrame(OpCode.TEXT).setFin(false));
        send.add(new WebSocketFrame(OpCode.CONTINUATION).setFin(false).setPayload("middle"));
        send.add(new WebSocketFrame(OpCode.CONTINUATION).setFin(true));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text("middle"));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 4 bytes
     */
    @Test
    public void testCase6_10_1() throws Exception
    {
        byte invalid[] = Hex.asByteArray("F7BFBFBF");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 5 bytes
     */
    @Test
    public void testCase6_10_2() throws Exception
    {
        byte invalid[] = Hex.asByteArray("FBBFBFBFBF");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 6 bytes
     */
    @Test
    public void testCase6_10_3() throws Exception
    {
        byte invalid[] = Hex.asByteArray("FDBFBFBFBFBF");
        assertBadTextPayload(invalid);
    }

    /**
     * valid utf8 text message, 1 frame/fragment. 3 bytes, 1 codepoint (other boundary conditions)
     */
    @Test
    public void testCase6_11_1() throws Exception
    {
        byte msg[] = Hex.asByteArray("ED9FBF");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment. 3 bytes, 1 codepoint (other boundary conditions)
     */
    @Test
    public void testCase6_11_2() throws Exception
    {
        byte msg[] = Hex.asByteArray("EE8080");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment. 3 bytes, 1 codepoint (other boundary conditions)
     */
    @Test
    public void testCase6_11_3() throws Exception
    {
        byte msg[] = Hex.asByteArray("EFBFBD");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment. 4 bytes, 1 codepoint (other boundary conditions)
     */
    @Test
    public void testCase6_11_4() throws Exception
    {
        byte msg[] = Hex.asByteArray("F48FBFBF");
        assertEchoTextMessage(msg);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 4 bytes (other boundary conditions)
     */
    @Test
    public void testCase6_11_5() throws Exception
    {
        byte invalid[] = Hex.asByteArray("F4908080");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 1 byte (unexpected continuation bytes)
     */
    @Test
    public void testCase6_12_1() throws Exception
    {
        byte invalid[] = Hex.asByteArray("80");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 1 byte (unexpected continuation bytes)
     */
    @Test
    public void testCase6_12_2() throws Exception
    {
        byte invalid[] = Hex.asByteArray("BF");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 2 bytes (unexpected continuation bytes)
     */
    @Test
    public void testCase6_12_3() throws Exception
    {
        byte invalid[] = Hex.asByteArray("80BF");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 3 bytes (unexpected continuation bytes)
     */
    @Test
    public void testCase6_12_4() throws Exception
    {
        byte invalid[] = Hex.asByteArray("80BF80");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 4 bytes (unexpected continuation bytes)
     */
    @Test
    public void testCase6_12_5() throws Exception
    {
        byte invalid[] = Hex.asByteArray("80BF80BF");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 5 bytes (unexpected continuation bytes)
     */
    @Test
    public void testCase6_12_6() throws Exception
    {
        byte invalid[] = Hex.asByteArray("80BF80BF80");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 6 bytes (unexpected continuation bytes)
     */
    @Test
    public void testCase6_12_7() throws Exception
    {
        byte invalid[] = Hex.asByteArray("80BF80BF80BF");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. many bytes (unexpected continuation bytes)
     */
    @Test
    public void testCase6_12_8() throws Exception
    {
        byte invalid[] = Hex.asByteArray("808182838485868788898A8B8C8D8E8F909192939495969798999A9B9C9D9E9"
                + "FA0A1A2A3A4A5A6A7A8A9AAABACADAEAFB0B1B2B3B4B5B6B7B8B9BABBBCBDBE");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. many bytes (lonely start characters)
     */
    @Test
    public void testCase6_13_1() throws Exception
    {
        byte invalid[] = Hex.asByteArray("C020C120C220C320C420C520C620C720C820C920CA20CB20CC20CD20CE20CF2"
                + "0D020D120D220D320D420D520D620D720D820D920DA20DB20DC20DD20DE20");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. many bytes (lonely start characters)
     */
    @Test
    public void testCase6_13_2() throws Exception
    {
        byte invalid[] = Hex.asByteArray("E020E120E220E320E420E520E620E720E820E920EA20EB20EC20ED20EE20");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. many bytes (lonely start characters)
     */
    @Test
    public void testCase6_13_3() throws Exception
    {
        byte invalid[] = Hex.asByteArray("F020F120F220F320F420F520F620");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 6 bytes (lonely start characters)
     */
    @Test
    public void testCase6_13_4() throws Exception
    {
        byte invalid[] = Hex.asByteArray("F820F920FA20");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment. 2 bytes (lonely start characters)
     */
    @Test
    public void testCase6_13_5() throws Exception
    {
        byte invalid[] = Hex.asByteArray("FC20");
        assertBadTextPayload(invalid);
    }

    /**
     * valid utf8 text message, 1 frame/fragment.
     */
    @Test
    public void testCase6_2_1() throws Exception
    {
        String utf8 = "Hello-\uC2B5@\uC39F\uC3A4\uC3BC\uC3A0\uC3A1-UTF-8!!";
        byte msg[] = StringUtil.getUtf8Bytes(utf8);
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 2 fragments (on UTF8 code point boundary)
     */
    @Test
    public void testCase6_2_2() throws Exception
    {
        String utf8[] =
        { "Hello-\uC2B5@\uC39F\uC3A4", "\uC3BC\uC3A0\uC3A1-UTF-8!!" };

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new WebSocketFrame(OpCode.TEXT).setPayload(utf8[0]).setFin(false));
        send.add(new WebSocketFrame(OpCode.CONTINUATION).setPayload(utf8[1]).setFin(true));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new WebSocketFrame(OpCode.TEXT).setPayload(utf8[0] + utf8[1]));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * valid utf8 text message, many fragments (1 byte each)
     */
    @Test
    public void testCase6_2_3() throws Exception
    {
        String utf8 = "Hello-\uC2B5@\uC39F\uC3A4\uC3BC\uC3A0\uC3A1-UTF-8!!";
        byte msg[] = StringUtil.getUtf8Bytes(utf8);

        List<WebSocketFrame> send = new ArrayList<>();
        fragmentText(send,msg);
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new WebSocketFrame(OpCode.TEXT).setPayload(msg));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * valid utf8 text message, many fragments (1 byte each)
     */
    @Test
    public void testCase6_2_4() throws Exception
    {
        byte msg[] = Hex.asByteArray("CEBAE1BDB9CF83CEBCCEB5");

        List<WebSocketFrame> send = new ArrayList<>();
        fragmentText(send,msg);
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new WebSocketFrame(OpCode.TEXT).setPayload(msg));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * invalid utf8 text message, 1 frame/fragments
     */
    @Test
    public void testCase6_3_1() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1BDB9CF83CEBCCEB5EDA080656469746564");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, many fragments (1 byte each)
     */
    @Test
    public void testCase6_3_2() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1BDB9CF83CEBCCEB5EDA080656469746564");

        List<WebSocketFrame> send = new ArrayList<>();
        fragmentText(send,invalid);
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.BAD_PAYLOAD).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * invalid text message, 3 fragments.
     * <p>
     * fragment #1 and fragment #3 are both valid in themselves.
     * <p>
     * fragment #2 contains the invalid utf8 code point.
     */
    @Test
    @Slow
    public void testCase6_4_1() throws Exception
    {
        byte part1[] = StringUtil.getUtf8Bytes("\u03BA\u1F79\u03C3\u03BC\u03B5");
        byte part2[] = Hex.asByteArray("F4908080"); // invalid
        byte part3[] = StringUtil.getUtf8Bytes("edited");

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.BAD_PAYLOAD).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);

            fuzzer.send(new WebSocketFrame(OpCode.TEXT).setPayload(part1).setFin(false));
            TimeUnit.SECONDS.sleep(1);
            fuzzer.send(new WebSocketFrame(OpCode.CONTINUATION).setPayload(part2).setFin(false));
            TimeUnit.SECONDS.sleep(1);
            fuzzer.send(new WebSocketFrame(OpCode.CONTINUATION).setPayload(part3).setFin(true));

            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * invalid text message, 3 fragments.
     * <p>
     * fragment #1 is valid and ends in the middle of an incomplete code point.
     * <p>
     * fragment #2 finishes the UTF8 code point but it is invalid
     * <p>
     * fragment #3 contains the remainder of the message.
     */
    @Test
    @Slow
    public void testCase6_4_2() throws Exception
    {
        byte part1[] = Hex.asByteArray("CEBAE1BDB9CF83CEBCCEB5F4"); // split code point
        byte part2[] = Hex.asByteArray("90"); // continue code point & invalid
        byte part3[] = Hex.asByteArray("8080656469746564"); // continue code point & finish

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.BAD_PAYLOAD).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(new WebSocketFrame(OpCode.TEXT).setPayload(part1).setFin(false));
            TimeUnit.SECONDS.sleep(1);
            fuzzer.send(new WebSocketFrame(OpCode.CONTINUATION).setPayload(part2).setFin(false));
            TimeUnit.SECONDS.sleep(1);
            fuzzer.send(new WebSocketFrame(OpCode.CONTINUATION).setPayload(part3).setFin(true));
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * invalid text message, 1 frame/fragment (slowly, and split within code points)
     */
    @Test
    @Slow
    public void testCase6_4_3() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1BDB9CF83CEBCCEB5F49080808080656469746564");

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new WebSocketFrame(OpCode.TEXT).setPayload(invalid));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.BAD_PAYLOAD).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();

            ByteBuffer net = fuzzer.asNetworkBuffer(send);
            fuzzer.send(net,6);
            fuzzer.send(net,11);
            TimeUnit.SECONDS.sleep(1);
            fuzzer.send(net,4);
            TimeUnit.SECONDS.sleep(1);
            fuzzer.send(net,100); // the rest

            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * invalid text message, 1 frame/fragment (slowly, and split within code points)
     */
    @Test
    @Slow
    public void testCase6_4_4() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1BDB9CF83CEBCCEB5F49080808080656469746564");

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new WebSocketFrame(OpCode.TEXT).setPayload(invalid));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.BAD_PAYLOAD).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();

            ByteBuffer net = fuzzer.asNetworkBuffer(send);
            fuzzer.send(net,6);
            fuzzer.send(net,11);
            TimeUnit.SECONDS.sleep(1);
            fuzzer.send(net,1);
            TimeUnit.SECONDS.sleep(1);
            fuzzer.send(net,100); // the rest

            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * valid utf8 text message, 1 frame/fragment.
     */
    @Test
    public void testCase6_5_1() throws Exception
    {
        byte msg[] = Hex.asByteArray("CEBAE1BDB9CF83CEBCCEB5");
        assertEchoTextMessage(msg);
    }

    /**
     * invalid utf8 (incomplete code point) text message, 1 frame/fragment.
     */
    @Test
    public void testCase6_6_1() throws Exception
    {
        byte incomplete[] = Hex.asByteArray("CE");
        assertBadTextPayload(incomplete);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 4 valid code points + 1 partial code point
     */
    @Test
    public void testCase6_6_10() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1BDB9CF83CEBCCE");
        assertBadTextPayload(invalid);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 5 valid code points (preserved on echo).
     */
    @Test
    public void testCase6_6_11() throws Exception
    {
        assertEchoTextMessage(Hex.asByteArray("CEBAE1BDB9CF83CEBCCEB5"));
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 1 valid code point (preserved on echo).
     */
    @Test
    public void testCase6_6_2() throws Exception
    {
        assertEchoTextMessage(Hex.asByteArray("CEBA"));
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 1 valid code point + 1 partial code point
     */
    @Test
    public void testCase6_6_3() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 1 valid code point + 1 invalid code point
     */
    @Test
    public void testCase6_6_4() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1BD");
        assertBadTextPayload(invalid);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 2 valid code points (preserved on echo).
     */
    @Test
    public void testCase6_6_5() throws Exception
    {
        byte msg[] = Hex.asByteArray("CEBAE1BDB9");
        assertEchoTextMessage(msg);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 2 valid code points + 1 partial code point
     */
    @Test
    public void testCase6_6_6() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1BDB9CF");
        assertBadTextPayload(invalid);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 3 valid code points (preserved on echo).
     */
    @Test
    public void testCase6_6_7() throws Exception
    {
        byte msg[] = Hex.asByteArray("CEBAE1BDB9CF83");
        assertEchoTextMessage(msg);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 3 valid code points + 1 partial code point
     */
    @Test
    public void testCase6_6_8() throws Exception
    {
        byte invalid[] = Hex.asByteArray("CEBAE1BDB9CF83CE");
        assertBadTextPayload(invalid);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 4 valid code points (preserved on echo).
     */
    @Test
    public void testCase6_6_9() throws Exception
    {
        byte msg[] = Hex.asByteArray("CEBAE1BDB9CF83CEBC");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 1 byte, 1 codepoint
     */
    @Test
    public void testCase6_7_1() throws Exception
    {
        byte msg[] = Hex.asByteArray("00");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 2 bytes, 1 codepoint
     */
    @Test
    public void testCase6_7_2() throws Exception
    {
        byte msg[] = Hex.asByteArray("C280");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 3 bytes, 1 codepoint
     */
    @Test
    public void testCase6_7_3() throws Exception
    {
        byte msg[] = Hex.asByteArray("E0A080");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 4 bytes, 1 codepoint
     */
    @Test
    public void testCase6_7_4() throws Exception
    {
        byte msg[] = Hex.asByteArray("F0908080");
        assertEchoTextMessage(msg);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 5 bytes
     */
    @Test
    public void testCase6_8_1() throws Exception
    {
        byte invalid[] = Hex.asByteArray("F888808080");
        assertBadTextPayload(invalid);
    }

    /**
     * invalid utf8 text message, 1 frame/fragment, 6 bytes
     */
    @Test
    public void testCase6_8_2() throws Exception
    {
        byte invalid[] = Hex.asByteArray("FC8480808080");
        assertBadTextPayload(invalid);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 1 byte, 1 codepoint
     */
    @Test
    public void testCase6_9_1() throws Exception
    {
        byte msg[] = Hex.asByteArray("7F");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 2 bytes, 1 codepoint
     */
    @Test
    public void testCase6_9_2() throws Exception
    {
        byte msg[] = Hex.asByteArray("DFBF");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 3 bytes, 1 codepoint
     */
    @Test
    public void testCase6_9_3() throws Exception
    {
        byte msg[] = Hex.asByteArray("EFBFBF");
        assertEchoTextMessage(msg);
    }

    /**
     * valid utf8 text message, 1 frame/fragment, 4 bytes, 1 codepoint
     */
    @Test
    public void testCase6_9_4() throws Exception
    {
        byte msg[] = Hex.asByteArray("F48FBFBF");
        assertEchoTextMessage(msg);
    }
}
