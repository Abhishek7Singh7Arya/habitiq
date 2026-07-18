import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  useColorScheme,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { useAuth } from '../context/AuthContext';
import { Colors } from '../constants/theme';
import { GlassCard } from '../components/GlassCard';
import axios from 'axios';

interface Message {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
}

export default function AIAssistantChat() {
  const { token, user, apiBaseUrl } = useAuth();
  const params = useLocalSearchParams<{ preloadedFileText?: string }>();
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMsg, setInputMsg] = useState('');
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [isSending, setIsSending] = useState(false);
  const [routineConfirmed, setRoutineConfirmed] = useState(false);

  const colorScheme = useColorScheme() ?? 'dark';
  const colors = Colors[colorScheme];
  const scrollViewRef = useRef<ScrollView>(null);

  useEffect(() => {
    if (params?.preloadedFileText) {
      startConversationWithFile(params.preloadedFileText);
    } else {
      setMessages([
        {
          id: 'welcome',
          role: 'ASSISTANT',
          content: `Hi ${user?.profile?.name || 'there'}! I'm your HabitIQ AI assistant.\n\nTo build your personalized weekly diet and workout routine, just describe your goals or say "Generate my routine".\n\nIf you have a routine file from another coach, upload it in the "Upload" tab!`,
          createdAt: new Date().toISOString(),
        },
      ]);
    }
  }, [params?.preloadedFileText]);

  const startConversationWithFile = async (fileText: string) => {
    if (!token || !user) return;
    setIsSending(true);
    try {
      const profileContext = user.profile
        ? `Name: ${user.profile.name}, Goal: ${user.profile.fitnessGoal}, Weight: ${user.profile.weightKg}kg, Height: ${user.profile.heightCm}cm`
        : 'None';

      const response = await axios.post(
        `${apiBaseUrl}/api/ai/chat/file`,
        { extractedText: fileText, userContext: profileContext },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-User-Id': user.id,
          },
        }
      );

      if (response.data?.success && response.data.data) {
        const { conversationId: newConvId, message: aiReply } = response.data.data;
        setConversationId(newConvId);
        setMessages([
          {
            id: 'file-upload-ack',
            role: 'USER',
            content: '📎 [Uploaded Routine Plan File]',
            createdAt: new Date().toISOString(),
          },
          {
            id: 'file-upload-reply',
            role: 'ASSISTANT',
            content: aiReply,
            createdAt: new Date().toISOString(),
          },
        ]);
      }
    } catch (e) {
      console.error('Failed to start file conversation', e);
      setMessages([
        {
          id: 'error',
          role: 'ASSISTANT',
          content: 'Oops! I encountered an error trying to analyze your file. Please try again or describe your goals manually.',
          createdAt: new Date().toISOString(),
        },
      ]);
    } finally {
      setIsSending(false);
    }
  };

  const handleSend = async (customMessage?: string) => {
    const textToSend = customMessage || inputMsg;
    if (!textToSend.trim() || isSending || !token || !user) return;

    if (!customMessage) setInputMsg('');
    setIsSending(true);

    const userMessage: Message = {
      id: Math.random().toString(),
      role: 'USER',
      content: textToSend,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    scrollToBottom();

    try {
      const profileContext = user.profile
        ? `Name: ${user.profile.name}, Goal: ${user.profile.fitnessGoal}, Weight: ${user.profile.weightKg}kg, Height: ${user.profile.heightCm}cm, Conditions: ${user.profile.healthConditions || 'None'}, Diet: ${user.profile.dietaryPreferences || 'None'}`
        : 'None';

      const response = await axios.post(
        `${apiBaseUrl}/api/ai/chat`,
        {
          conversationId: conversationId || undefined,
          message: textToSend,
          userContext: profileContext,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-User-Id': user.id,
          },
        }
      );

      if (response.data?.success && response.data.data) {
        const { conversationId: updatedConvId, message: aiReply, routineConfirmed: isConfirmed } = response.data.data;
        if (!conversationId) setConversationId(updatedConvId);
        if (isConfirmed) setRoutineConfirmed(true);

        const aiResponse: Message = {
          id: Math.random().toString(),
          role: 'ASSISTANT',
          content: aiReply,
          createdAt: new Date().toISOString(),
        };

        setMessages((prev) => [...prev, aiResponse]);
        scrollToBottom();
      }
    } catch (e) {
      console.error('Chat failed', e);
      setMessages((prev) => [
        ...prev,
        {
          id: Math.random().toString(),
          role: 'ASSISTANT',
          content: 'I had trouble connecting. Let\'s try sending that again.',
          createdAt: new Date().toISOString(),
        },
      ]);
    } finally {
      setIsSending(false);
    }
  };

  const scrollToBottom = () => {
    setTimeout(() => {
      scrollViewRef.current?.scrollToEnd({ animated: true });
    }, 100);
  };

  const containsSatisfactionOffer = () => {
    if (messages.length === 0) return false;
    const lastMsg = messages[messages.length - 1];
    return (
      lastMsg.role === 'ASSISTANT' &&
      lastMsg.content.toLowerCase().includes('are you satisfied with this routine')
    );
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
      style={[styles.container, { backgroundColor: colors.background }]}
    >
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: colors.text }]}>AI Habit Coach</Text>
        <Text style={[styles.headerSubtitle, { color: colors.textSecondary }]}>
          Build & refine your personal routine interactively
        </Text>
      </View>

      <ScrollView
        ref={scrollViewRef}
        contentContainerStyle={styles.scrollContent}
        onContentSizeChange={scrollToBottom}
      >
        {messages.map((msg) => {
          const isUser = msg.role === 'USER';
          return (
            <View
              key={msg.id}
              style={[styles.bubbleWrapper, isUser ? styles.userWrapper : styles.assistantWrapper]}
            >
              <GlassCard
                style={[
                  styles.bubble,
                  isUser
                    ? { backgroundColor: colors.accent, borderColor: colors.accent }
                    : { backgroundColor: 'rgba(255,255,255,0.03)', borderColor: colors.cardBorder },
                ]}
              >
                <Text style={[styles.bubbleText, { color: isUser ? '#fff' : colors.text }]}>
                  {msg.content}
                </Text>
              </GlassCard>
            </View>
          );
        })}

        {isSending && (
          <View style={[styles.bubbleWrapper, styles.assistantWrapper]}>
            <GlassCard style={[styles.bubble, { backgroundColor: 'rgba(255,255,255,0.03)', borderColor: colors.cardBorder }]}>
              <ActivityIndicator color={colors.accent} size="small" />
            </GlassCard>
          </View>
        )}

        {routineConfirmed && (
          <GlassCard style={[styles.confirmedCard, { borderColor: colors.accent }]}>
            <Text style={[styles.confirmedTitle, { color: colors.text }]}>🎉 Routine Confirmed!</Text>
            <Text style={[styles.confirmedDesc, { color: colors.textSecondary }]}>
              Your personalized workout and diet routine is locked in. Head back to the **Timeline** tab to see your schedule and start tracking!
            </Text>
          </GlassCard>
        )}

        {!routineConfirmed && containsSatisfactionOffer() && (
          <GlassCard style={styles.reviewCard}>
            <Text style={[styles.reviewTitle, { color: colors.text }]}>Routine Satisfaction Check</Text>
            <Text style={[styles.reviewDesc, { color: colors.textSecondary }]}>
              Tap below to lock this routine and start tracking, or type adjustments in the box!
            </Text>
            <View style={styles.reviewActions}>
              <TouchableOpacity
                style={[styles.reviewBtn, { backgroundColor: colors.accent }]}
                onPress={() => handleSend('YES')}
                disabled={isSending}
              >
                <Text style={styles.reviewBtnText}>✓ Confirm & Track</Text>
              </TouchableOpacity>
            </View>
          </GlassCard>
        )}
      </ScrollView>

      {!routineConfirmed && (
        <View style={[styles.inputBar, { borderTopColor: colors.cardBorder, backgroundColor: colors.background }]}>
          <TextInput
            style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: 'rgba(255,255,255,0.03)' }]}
            placeholder="Type goal details or refinements..."
            placeholderTextColor={colors.textSecondary}
            value={inputMsg}
            onChangeText={setInputMsg}
            onSubmitEditing={() => handleSend()}
            editable={!isSending}
          />
          <TouchableOpacity
            style={[styles.sendBtn, { backgroundColor: colors.accent }]}
            onPress={() => handleSend()}
            disabled={isSending}
          >
            <Text style={styles.sendBtnText}>Send</Text>
          </TouchableOpacity>
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    paddingHorizontal: 24,
    paddingTop: 64,
    paddingBottom: 16,
    gap: 4,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    letterSpacing: -0.5,
  },
  headerSubtitle: {
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '500',
  },
  scrollContent: {
    padding: 20,
    paddingBottom: 40,
  },
  bubbleWrapper: {
    flexDirection: 'row',
    marginVertical: 4,
    maxWidth: '85%',
  },
  userWrapper: {
    alignSelf: 'flex-end',
  },
  assistantWrapper: {
    alignSelf: 'flex-start',
  },
  bubble: {
    borderRadius: 16,
    padding: 12,
    borderWidth: 1,
  },
  bubbleText: {
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '500',
  },
  inputBar: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderTopWidth: 1,
    gap: 12,
    alignItems: 'center',
  },
  input: {
    flex: 1,
    height: 40,
    borderWidth: 1,
    borderRadius: 20,
    paddingHorizontal: 16,
    fontSize: 14,
  },
  sendBtn: {
    height: 38,
    paddingHorizontal: 16,
    borderRadius: 19,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendBtnText: {
    color: '#fff',
    fontSize: 13,
    fontWeight: 'bold',
  },
  reviewCard: {
    padding: 16,
    marginVertical: 12,
    gap: 8,
  },
  reviewTitle: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  reviewDesc: {
    fontSize: 11,
    lineHeight: 15,
  },
  reviewActions: {
    flexDirection: 'row',
    marginTop: 8,
  },
  reviewBtn: {
    flex: 1,
    height: 38,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  reviewBtnText: {
    color: '#fff',
    fontSize: 13,
    fontWeight: 'bold',
  },
  confirmedCard: {
    padding: 24,
    marginVertical: 16,
    borderWidth: 1,
    alignItems: 'center',
    gap: 8,
  },
  confirmedTitle: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  confirmedDesc: {
    fontSize: 12,
    lineHeight: 18,
    textAlign: 'center',
  },
});
