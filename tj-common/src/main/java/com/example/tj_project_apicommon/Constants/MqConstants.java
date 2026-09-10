package com.example.tj_project_apicommon.Constants;

public interface MqConstants {
    /**
     * 绑定名称（与 application.yml 中 spring.cloud.stream.bindings 的 key 对应）。
     * 命名规范：<业务>-out-0 表示生产者，<业务>-in-0 表示消费者。
     */
    interface Binding {
        /* 课程相关 */
        String COURSE_NEW_OUT = "courseNew-out-0";
        String COURSE_UP_OUT = "courseUp-out-0";
        String COURSE_DOWN_OUT = "courseDown-out-0";
        String COURSE_EXPIRE_OUT = "courseExpire-out-0";
        String COURSE_DELETE_OUT = "courseDelete-out-0";

        /* 订单相关 */
        String ORDER_PAY_OUT = "orderPay-out-0";
        String ORDER_REFUND_OUT = "orderRefund-out-0";
        String ORDER_DELAY_OUT = "orderDelay-out-0";

        /* 学习相关（消费者） */
        String LEARNING_IN = "learning-in-0";

        /* 短信相关 */
        String SMS_OUT = "sms-out-0";

        /* 异常消息 */
        String ERROR_OUT = "error-out-0";
        String ERROR_IN = "error-in-0";

        /* 支付相关 */
        String PAY_SUCCESS_OUT = "paySuccess-out-0";
        String REFUND_CHANGE_OUT = "refundChange-out-0";

        /* 点赞相关 */
        String LIKE_RECORD_OUT = "likeRecord-out-0";

        /* 优惠促销 */
        String COUPON_RECEIVE_OUT = "couponReceive-out-0";

        /* 积分相关 */
        String WRITE_REPLY_OUT = "writeReply-out-0";
        String SIGN_IN_OUT = "signIn-out-0";
        String LEARN_SECTION_OUT = "learnSection-out-0";
        String WRITE_NOTE_OUT = "writeNote-out-0";
        String NOTE_GATHERED_OUT = "noteGathered-out-0";
    }

    /**
     * 目标交换机名称（对应 YAML 中的 destination）。
     * 注意：SCSt 中交换机的声明由 Binder 自动完成，无需手写 @Bean。
     */
    interface Exchange {
        /* 课程有关的交换机 */
        String COURSE_EXCHANGE = "course.topic";
        /* 订单有关的交换机 */
        String ORDER_EXCHANGE = "order.topic";
        /* 学习有关的交换机 */
        String LEARNING_EXCHANGE = "learning.topic";
        /* 信息中心短信相关的交换机 */
        String SMS_EXCHANGE = "sms.direct";
        /* 异常信息的交换机 */
        String ERROR_EXCHANGE = "error.topic";
        /* 支付有关的交换机 */
        String PAY_EXCHANGE = "pay.topic";
        /* 交易服务延迟任务交换机 */
        String TRADE_DELAY_EXCHANGE = "trade.delay.topic";
        /* 点赞记录有关的交换机 */
        String LIKE_RECORD_EXCHANGE = "like.record.topic";
        /* 优惠促销有关的交换机 */
        String PROMOTION_EXCHANGE = "promotion.topic";
    }

    /**
     * 消费者组（对应 YAML 中 group）。
     * 相同组的多个实例将轮询消费；不同组会各自收到一份消息（广播）。
     */
    interface Group {
        String ORDER_SERVICE_GROUP = "order-service-group";
        String LEARNING_SERVICE_GROUP = "learning-service-group";
        String SMS_SERVICE_GROUP = "sms-service-group";
        String ERROR_SERVICE_GROUP = "error-service-group";
    }

    /**
     * 路由键（对应 YAML 中 routing-key-expression 的值）。
     * 注意：使用 SpEL 引用时需加单引号包裹，如 '''order.pay'''。
     */
    interface Key {
        /* 课程有关的 RoutingKey */
        String COURSE_NEW_KEY = "course.new";
        String COURSE_UP_KEY = "course.up";
        String COURSE_DOWN_KEY = "course.down";
        String COURSE_EXPIRE_KEY = "course.expire";
        String COURSE_DELETE_KEY = "course.delete";

        /* 订单有关的 RoutingKey */
        String ORDER_PAY_KEY = "order.pay";
        String ORDER_REFUND_KEY = "order.refund";
        String ORDER_DELAY_KEY = "delay.order.query";

        /* 积分相关 RoutingKey */
        String WRITE_REPLY = "reply.new";
        String SIGN_IN = "sign.in";
        String LEARN_SECTION = "section.learned";
        String WRITE_NOTE = "note.new";
        String NOTE_GATHERED = "note.gathered";

        /* 点赞相关 */
        String LIKED_TIMES_KEY_TEMPLATE = "{}.times.changed";
        String QA_LIKED_TIMES_KEY = "QA.times.changed";
        String NOTE_LIKED_TIMES_KEY = "NOTE.times.changed";

        /* 短信 */
        String SMS_MESSAGE = "sms.message";

        /* 异常相关 */
        String ERROR_KEY_PREFIX = "error.";
        String DEFAULT_ERROR_KEY = "error.#";

        /* 支付相关 */
        String PAY_SUCCESS = "pay.success";
        String REFUND_CHANGE = "refund.status.change";

        /* 优惠券 */
        String COUPON_RECEIVE = "coupon.receive";
    }
}
