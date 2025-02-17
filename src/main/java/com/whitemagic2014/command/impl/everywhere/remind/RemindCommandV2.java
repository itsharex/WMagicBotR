package com.whitemagic2014.command.impl.everywhere.remind;

import com.github.WhiteMagic2014.gptApi.Chat.pojo.ChatMessage;
import com.whitemagic2014.annotate.Command;
import com.whitemagic2014.annotate.Switch;
import com.whitemagic2014.command.impl.everywhere.BaseEveryWhereCommand;
import com.whitemagic2014.dic.Dic;
import com.whitemagic2014.pojo.CommandProperties;
import com.whitemagic2014.service.ChatPGTService;
import com.whitemagic2014.service.RemindService;
import com.whitemagic2014.util.DateFormatUtil;
import com.whitemagic2014.util.MagicHelper;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * @Description: 预约功能v2 使用gpt解析自然语言,需要开启gpt使用
 * @author: magic chen
 * @date: 2023/4/17 10:41
 **/
@Command
@Switch(name = Dic.Component_ChatGPT)
public class RemindCommandV2 extends BaseEveryWhereCommand {

    @Autowired
    RemindService remindService;

    @Autowired
    ChatPGTService chatPGTService;

    private List<String> okPool = Arrays.asList("好的", "ok", "知道了");

    private String getOk() {
        return okPool.get(MagicHelper.randomInt(okPool.size()));
    }

    public CommandProperties properties() {
        return new CommandProperties("备忘V2", "备忘2");
    }

    @Override
    public Message execute(User sender, ArrayList<String> args, MessageChain messageChain, Contact subject) throws Exception {
        Date now = new Date();
        String prompt = args.stream().map(s -> {
            return s.concat(" ");
        }).reduce("", String::concat);
        List<ChatMessage> voList = new ArrayList<>();
        voList.add(ChatMessage.systemMessage("请将给出的内容归纳为以下格式: 备忘 yyyy-MM-dd/HH:mm:ss 内容"));
        voList.add(ChatMessage.userMessage("当前时间为2023年04月13日11:26:02;3天后早上8点提醒我去机场接我的朋友ammy"));
        voList.add(ChatMessage.assistantMessage("备忘 2023-04-16/08:00:00 记得去机场接你的朋友ammy"));
        voList.add(ChatMessage.userMessage("当前时间为2023年04月13日11:26:02;2023年05月10日是peter的生日，记得提前提醒我为他准备生日礼物"));
        voList.add(ChatMessage.assistantMessage("备忘 2023-05-07/00:00:00 距离peter的生日只剩下3天了，记得准备好生日礼物。"));
        voList.add(ChatMessage.userMessage("当前时间为" + DateFormatUtil.sdfv4.format(now) + ";" + prompt));
        String result = chatPGTService.originChat(voList);
        // gpt解析结果不符
        if (!result.startsWith("备忘 ")) {
            return new PlainText("解析失败：" + result);
        }
        // 处理指令
        result = result.replaceFirst("备忘 ", "");
        String dateStr = result.substring(0, 19);  // 截取 yyyy-MM-dd/HH:mm:ss
        String msg = result.substring(20);// 截取后面的msg
        Date date;
        try {
            date = DateFormatUtil.sdfv3.parse(dateStr);
        } catch (ParseException pe) {
            return new PlainText("解析失败：" + result);
        }
        if (date.before(now)) {
            return new PlainText("解析失败：" + result);
        }

        String taskKey = "";
        if (subject instanceof Group) {
            taskKey = remindService.groupRemind(subject.getId(), sender.getId(), msg, date);
        } else if (subject instanceof User) {
            taskKey = remindService.friendRemind(sender.getId(), msg, date);
        }
        return new PlainText(getOk() + ",已经创建" + result + "\n备忘id=" + taskKey);
    }


}
