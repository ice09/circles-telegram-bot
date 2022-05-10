package dev.iot.telegrambot.telegramraspi.service;

import dev.iot.telegrambot.telegramraspi.service.dto.event.*;
import dev.iot.telegrambot.telegramraspi.web3.Web3Reader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class Web3TransactionCheckerTest {

    @Test
    public void shouldSkipWithEmptyMapAndNoTrackRequest() throws URISyntaxException, IOException {
        Web3Reader web3Reader = Mockito.mock(Web3Reader.class);
        GraphQLClient graphQLClient = Mockito.mock(GraphQLClient.class);
        when(web3Reader.getCurrentBlock()).thenReturn(BigInteger.ONE);
        Payload payload = Payload.builder().flow(BigInteger.TEN).from("0x0").to("0x1").build();
        Event event = Event.builder().block_number(BigInteger.ONE).payload(payload).build();
        List<Event> events = List.of(event);
        Data data = Data.builder().events(events).build();
        EventResponseDto eventResponseDto = EventResponseDto.builder().data(data).build();
        when(graphQLClient.trackAccount("0x0")).thenReturn(eventResponseDto);

        Web3TransactionChecker web3TransactionChecker = new Web3TransactionChecker(web3Reader, graphQLClient);
        web3TransactionChecker.checkTransactions();

        verifyNoInteractions(web3Reader, graphQLClient);
    }

    @Test
    public void shouldNotSkipWithEmptyMapAndTrackRequest() throws URISyntaxException, IOException {
        Web3Reader web3Reader = Mockito.mock(Web3Reader.class);
        GraphQLClient graphQLClient = Mockito.mock(GraphQLClient.class);
        when(web3Reader.getCurrentBlock()).thenReturn(BigInteger.ONE);
        Payload payload = Payload.builder().flow(BigInteger.TEN).from("0x0").to("0x1").build();
        Event event = Event.builder().block_number(BigInteger.ONE).payload(payload).build();
        List<Event> events = List.of(event);
        Data data = Data.builder().events(events).build();
        EventResponseDto eventResponseDto = EventResponseDto.builder().data(data).build();
        when(graphQLClient.trackAccount(any())).thenReturn(eventResponseDto);
        BotSender botSender = Mockito.mock(BotSender.class);

        CachedTransactionDto ctDto = CachedTransactionDto.builder().chatId("chatId").from("from").to("to").toAddr("toAddr").startBlock(BigInteger.ONE).build();

        Web3TransactionChecker web3TransactionChecker = new Web3TransactionChecker(web3Reader, graphQLClient);
        web3TransactionChecker.trackAccount("chatId", "safe", "from", "toAddr", "to", botSender);
        web3TransactionChecker.checkTransactions();

        Map<String, List<CachedTransactionDto>> watchedTransactions = (Map<String, List<CachedTransactionDto>>) ReflectionTestUtils.getField(web3TransactionChecker, "watchedTransactions");

        verify(web3Reader, times(2)).getCurrentBlock();
        verify(graphQLClient, times(1)).trackAccount(anyString());

        assertThat(watchedTransactions).containsEntry("safe", List.of(ctDto));
    }

    @Test
    public void shouldRemoveOldBlock() throws URISyntaxException, IOException {
        Web3Reader web3Reader = Mockito.mock(Web3Reader.class);
        GraphQLClient graphQLClient = Mockito.mock(GraphQLClient.class);
        when(web3Reader.getCurrentBlock()).thenReturn(BigInteger.ONE);
        Payload payload = Payload.builder().flow(BigInteger.TEN).from("0x0").to("0x1").build();
        Event event = Event.builder().block_number(BigInteger.TWO).payload(payload).build();
        List<Event> events = List.of(event);
        Data data = Data.builder().events(events).build();
        EventResponseDto eventResponseDto = EventResponseDto.builder().data(data).build();
        when(graphQLClient.trackAccount(any())).thenReturn(eventResponseDto);
        BotSender botSender = Mockito.mock(BotSender.class);

        Web3TransactionChecker web3TransactionChecker = new Web3TransactionChecker(web3Reader, graphQLClient);
        web3TransactionChecker.trackAccount("chatId", "safe", "from", "0x1", "to", botSender);
        web3TransactionChecker.checkTransactions();

        Map<String, List<CachedTransactionDto>> watchedTransactions = (Map<String, List<CachedTransactionDto>>) ReflectionTestUtils.getField(web3TransactionChecker, "watchedTransactions");

        verify(web3Reader, times(2)).getCurrentBlock();
        verify(graphQLClient, times(1)).trackAccount(anyString());

        assertThat(watchedTransactions).isEmpty();
    }

    @Test
    public void shouldNotRemoveOldBlock() throws URISyntaxException, IOException {
        Web3Reader web3Reader = Mockito.mock(Web3Reader.class);
        GraphQLClient graphQLClient = Mockito.mock(GraphQLClient.class);
        when(web3Reader.getCurrentBlock()).thenReturn(BigInteger.TWO);
        Payload payload = Payload.builder().flow(BigInteger.TEN).from("0x0").to("0x1").build();
        Event event = Event.builder().block_number(BigInteger.ONE).payload(payload).build();
        List<Event> events = List.of(event);
        Data data = Data.builder().events(events).build();
        EventResponseDto eventResponseDto = EventResponseDto.builder().data(data).build();
        when(graphQLClient.trackAccount(any())).thenReturn(eventResponseDto);
        BotSender botSender = Mockito.mock(BotSender.class);

        CachedTransactionDto ctDto = CachedTransactionDto.builder().chatId("chatId").from("from").to("to").toAddr("0x1").startBlock(BigInteger.TWO).build();

        Web3TransactionChecker web3TransactionChecker = new Web3TransactionChecker(web3Reader, graphQLClient);
        web3TransactionChecker.trackAccount("chatId", "safe", "from", "0x1", "to", botSender);
        web3TransactionChecker.checkTransactions();

        Map<String, List<CachedTransactionDto>> watchedTransactions = (Map<String, List<CachedTransactionDto>>) ReflectionTestUtils.getField(web3TransactionChecker, "watchedTransactions");

        verify(web3Reader, times(2)).getCurrentBlock();
        verify(graphQLClient, times(1)).trackAccount(anyString());

        assertThat(watchedTransactions).containsEntry("safe", List.of(ctDto));
    }

    @Test
    public void shouldRemoveOldEventAfter11Blocks() throws URISyntaxException, IOException {
        Web3Reader web3Reader = Mockito.mock(Web3Reader.class);
        GraphQLClient graphQLClient = Mockito.mock(GraphQLClient.class);
        when(web3Reader.getCurrentBlock()).thenReturn(BigInteger.ONE).thenReturn(BigInteger.valueOf(12));
        Payload payload = Payload.builder().flow(BigInteger.TEN).from("0x0").to("0x1").build();
        Event event1 = Event.builder().block_number(BigInteger.valueOf(1)).payload(payload).build();
        List<Event> events1 = List.of(event1);
        Data data1 = Data.builder().events(events1).build();
        EventResponseDto eventResponseDto1 = EventResponseDto.builder().data(data1).build();
        when(graphQLClient.trackAccount(any())).thenReturn(eventResponseDto1);
        BotSender botSender = Mockito.mock(BotSender.class);

        Web3TransactionChecker web3TransactionChecker = new Web3TransactionChecker(web3Reader, graphQLClient);
        web3TransactionChecker.trackAccount("chatId", "safe", "from", "0x1", "to", botSender);
        web3TransactionChecker.checkTransactions();

        Map<String, List<CachedTransactionDto>> watchedTransactions = (Map<String, List<CachedTransactionDto>>) ReflectionTestUtils.getField(web3TransactionChecker, "watchedTransactions");

        verify(web3Reader, times(2)).getCurrentBlock();
        verify(graphQLClient, times(1)).trackAccount(anyString());

        assertThat(watchedTransactions).isEmpty();
    }

    @Test
    public void shouldNotRemoveOldEventAfter9Blocks() throws URISyntaxException, IOException {
        Web3Reader web3Reader = Mockito.mock(Web3Reader.class);
        GraphQLClient graphQLClient = Mockito.mock(GraphQLClient.class);
        when(web3Reader.getCurrentBlock()).thenReturn(BigInteger.ONE).thenReturn(BigInteger.valueOf(10));
        Payload payload = Payload.builder().flow(BigInteger.TEN).from("0x0").to("0x1").build();
        Event event1 = Event.builder().block_number(BigInteger.valueOf(1)).payload(payload).build();
        List<Event> events1 = List.of(event1);
        Data data1 = Data.builder().events(events1).build();
        EventResponseDto eventResponseDto1 = EventResponseDto.builder().data(data1).build();
        when(graphQLClient.trackAccount(any())).thenReturn(eventResponseDto1);
        BotSender botSender = Mockito.mock(BotSender.class);

        CachedTransactionDto ctDto = CachedTransactionDto.builder().chatId("chatId").from("from").to("to").toAddr("0x1").startBlock(BigInteger.ONE).build();

        Web3TransactionChecker web3TransactionChecker = new Web3TransactionChecker(web3Reader, graphQLClient);
        web3TransactionChecker.trackAccount("chatId", "safe", "from", "0x1", "to", botSender);
        web3TransactionChecker.checkTransactions();

        Map<String, List<CachedTransactionDto>> watchedTransactions = (Map<String, List<CachedTransactionDto>>) ReflectionTestUtils.getField(web3TransactionChecker, "watchedTransactions");

        verify(web3Reader, times(2)).getCurrentBlock();
        verify(graphQLClient, times(1)).trackAccount(anyString());

        assertThat(watchedTransactions).containsEntry("safe", List.of(ctDto));
    }
}
