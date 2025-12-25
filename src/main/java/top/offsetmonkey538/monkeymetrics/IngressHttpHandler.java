package top.offsetmonkey538.monkeymetrics;

import com.google.gson.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public final class IngressHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx);
            return;
        }

        handleIngest(ctx, request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO: Do I really want to blame the requester for any exceptions? Do I want to give them the full error?
        //sendError(ctx, null, INTERNAL_SERVER_ERROR, cause.toString());
        sendError(ctx, BAD_REQUEST, cause.toString());
        cause.printStackTrace();
    }

    private static void handleIngest(@NotNull ChannelHandlerContext ctx, @NotNull FullHttpRequest request) {
        if (request.method() != HttpMethod.POST) {
            sendError(ctx, METHOD_NOT_ALLOWED, "Only POST requests are allowed on this path");
            return;
        }
        final String contentType = request.headers().get(CONTENT_TYPE);
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            sendError(ctx, UNSUPPORTED_MEDIA_TYPE, "Content type must be set to application/json");
            return;
        }

        final JsonObject json;
        try {
            json = JsonParser.parseString(request.content().toString(StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            sendError(ctx, BAD_REQUEST, e.toString());
            return;
        }

        final List<String> errors = validateJson(json);
        if (!errors.isEmpty()) {
            sendError(ctx, BAD_REQUEST, String.join("\n", errors));
            return;
        }

        final String minecraftVersion = normalize(json.get("minecraft").getAsString());
        final String environment = switch (normalize(json.get("env").getAsString())) {
            case "c" -> "client";
            case "s" -> "server";
            default -> throw new RuntimeException("Unexpected value in 'env'");
        };
        final String modLoader = normalize(json.get("loader").getAsString());
        final JsonArray mods = json.get("mods").getAsJsonArray();

        Main.ENVIRONMENT_COUNTER.labelValues(
                minecraftVersion,
                environment,
                modLoader
        ).inc();

        for (JsonElement modEntry : mods) {
            Main.MOD_COUNTER.labelValues(
                    normalize(modEntry.getAsString()),
                    minecraftVersion,
                    environment,
                    modLoader
            ).inc();
        }

        sendSuccess(ctx);
    }

    private static List<String> validateJson(final JsonObject json) {
        final List<String> result = new ArrayList<>(3);

        if (!json.has("minecraft")) result.add("'minecraft' field is missing!");
        if (!json.has("env")) result.add("'env' field is missing!");
        if (!json.has("loader")) result.add("'loader' field is missing!");
        if (!json.has("mods")) result.add("'mods' field is missing!");

        return result;
    }

    private static String normalize(final @NotNull String value) {
        return value.toLowerCase().replaceAll("[_ ]", "-");
    }


    private static void sendError(@NotNull ChannelHandlerContext ctx) {
        sendError(ctx, HttpResponseStatus.BAD_REQUEST, null);
    }

    private static void sendError(@NotNull ChannelHandlerContext ctx, @NotNull HttpResponseStatus status, @Nullable String reason) {
        final ByteBuf byteBuf = Unpooled.copiedBuffer(reason == null ? status.toString() : status + "\n" + reason, CharsetUtil.UTF_8);
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, byteBuf);
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        sendResponse(ctx, response);
    }

    private static void sendSuccess(@NotNull ChannelHandlerContext ctx) {
        sendResponse(ctx, new DefaultFullHttpResponse(HTTP_1_1, OK));
    }

    private static void sendResponse(@NotNull ChannelHandlerContext ctx, @NotNull FullHttpResponse response) {
        response.headers().set(CONNECTION, CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
