package com.zzyl;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class QianfanAIModelTest {

    public static void main(String[] args) {

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey("bce-v3/ALTAK-zUImUWbA7zgPV6UsgPtXH/22005700e61f66e124980c265e6a12ddad53aa05")
                .baseUrl("https://qianfan.baidubce.com/v2")
                .build();

        ChatCompletionCreateParams params =
                ChatCompletionCreateParams.builder()
                        .model("ernie-5.1")
                        .addUserMessage(
                                "请返回一个 JSON 对象，必须包含 name、age、city 三个字段。" +
                                        "不要输出 Markdown 代码块，不要添加解释文字。"
                        )
                        .responseFormat(
                                ChatCompletionCreateParams.ResponseFormat.ofJsonObject(
                                        ResponseFormatJsonObject.builder().build()
                                )
                        )
                        .build();

        ChatCompletion completion =
                client.chat().completions().create(params);

        String content = completion.choices()
                .get(0)
                .message()
                .content()
                .orElseThrow(() -> new IllegalStateException("模型未返回内容"));

        System.out.println(content);
    }
}