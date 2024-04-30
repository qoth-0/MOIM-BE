package com.team1.moim.domain.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // websocket stomp로 연결하는 흐름에 대한 제어를 위한 interceptor
    // JWT 인증을 위해 사용
    private final StompHandler stompHandler;

    //StompExceptionHandler는 websocket 연결 시 터지는 exception을 핸들링하기 위한 클래스
    private final StompExceptionHandler stompExceptionHandler;

    /**
     * WebSocketMessageBrokerConfigurer 인터페이스의 기본 메서드를 구현하여 메시지 브로커를 구성
     * @param registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 웹소켓을 사용할 때 새로운 메시지 브로커를 만들어
        // destinationPrefixes(e.g., "/sub") 로 들어오는 모든 주소로 메시지를 전송한다.
        registry.enableSimpleBroker("/sub");
        // @Messagemapping 주석이 달린 메서드에 바인딩된 메시지의 "/pub" 접두사를 지정한다.
        // prefix를 "/pub"으로 설정한다면 "/sub/hello" 라는 토픽에 구독을 신청했을 때
        // 실제 경로는 "/pub/sub/hello"가 된다.
        // 아래의 접두사는 모든 메시지 매핑을 정의하는데 사용됨.
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // "/websocket-endpoint" 엔드포인트를 등록함으로써 WebSocket을 사용할 수 없는 경우
        // 대체 전송을 사용할 수 있도록 SockJS 폴백 옵션을 활성화한다.
        // SockJS 클라이언트는 "/ws-endpoint"에 연결을 시도하고 사용 가능한 최상의 전송을 사용
        // 즉, registerStompEndpoints 메서드는 연결할 소켓 엔드포인트를 등록하는 메서드
        registry.setErrorHandler(stompExceptionHandler)
                .addEndpoint("/ws-endpoint")
//                .addInterceptors()
                .setAllowedOriginPatterns("*")
                // 클라이언트가 sockJS로 개발되었을 때만 필요하다(필요 없으면 추후 제거)
                .withSockJS();
    }

    // TCP handshake 시 JWT 인증을 위함. 처음 연결될 때 JWT를 이용해서 단 한 번 유효한 유저인가 판단한다.
    // 클라이언트로부터 들어오는 메시지를 처리하는 MessageChannel을 구성하는 역할을 한다.
    // registration.interceptors 메서드를 사용해서 STOMP 메시지 처리를 구성하는 메시지 채널에 custom한 인터셉터를 추가 구성하여
    // 채널의 현재 인터셉터 목록에 추가하는 단계를 거친다.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler);
    }
}
