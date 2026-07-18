import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  useColorScheme,
  RefreshControl,
} from 'react-native';
import { useAuth } from '../context/AuthContext';
import { Colors } from '../constants/theme';
import { GlassCard } from '../components/GlassCard';
import axios from 'axios';

interface RoutineTask {
  id: string;
  taskType: string;
  scheduledTime: string;
  description: string;
  durationMinutes?: number;
  notes?: string;
  completed?: boolean;
  skipped?: boolean;
}

interface RoutineDay {
  id: string;
  dayOrder: number;
  dayLabel: string;
  notes?: string;
  tasks: RoutineTask[];
}

interface Routine {
  id: string;
  title: string;
  description?: string;
  days: RoutineDay[];
}

export default function TrackerDashboard() {
  const { token, user, apiBaseUrl, logout } = useAuth();
  const [activeRoutine, setActiveRoutine] = useState<Routine | null>(null);
  const [todayTasks, setTodayTasks] = useState<RoutineTask[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const colorScheme = useColorScheme() ?? 'dark';
  const colors = Colors[colorScheme];

  const fetchActiveRoutine = async () => {
    if (!token || !user) return;
    setError(null);
    try {
      const response = await axios.get(`${apiBaseUrl}/api/ai/routines/active`, {
        headers: {
          Authorization: `Bearer ${token}`,
          'X-User-Id': user.id,
        },
      });

      if (response.data?.success && response.data.data) {
        const routine: Routine = response.data.data;
        setActiveRoutine(routine);
        resolveTodayTasks(routine);
      } else {
        setActiveRoutine(null);
      }
    } catch (e: any) {
      if (e.response?.status !== 404) {
        setError('Could not sync active tracker. Please swipe down to refresh.');
      }
      setActiveRoutine(null);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  const resolveTodayTasks = (routine: Routine) => {
    const daysOfWeek = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const currentDayName = daysOfWeek[new Date().getDay()];

    const dayMatch = routine.days.find(
      (d) => d.dayLabel.toLowerCase().trim() === currentDayName.toLowerCase()
    );

    if (dayMatch) {
      const enrichedTasks = dayMatch.tasks.map((t) => ({
        ...t,
        completed: false,
        skipped: false,
      }));
      setTodayTasks(enrichedTasks);
    } else if (routine.days.length > 0) {
      const enrichedTasks = routine.days[0].tasks.map((t) => ({
        ...t,
        completed: false,
        skipped: false,
      }));
      setTodayTasks(enrichedTasks);
    } else {
      setTodayTasks([]);
    }
  };

  useEffect(() => {
    fetchActiveRoutine();
  }, []);

  const handleRefresh = () => {
    setIsRefreshing(true);
    fetchActiveRoutine();
  };

  const toggleComplete = (taskId: string) => {
    setTodayTasks((prev) =>
      prev.map((t) =>
        t.id === taskId ? { ...t, completed: !t.completed, skipped: false } : t
      )
    );
  };

  const toggleSkip = (taskId: string) => {
    setTodayTasks((prev) =>
      prev.map((t) =>
        t.id === taskId ? { ...t, skipped: !t.skipped, completed: false } : t
      )
    );
  };

  const getCompletionPercent = () => {
    if (todayTasks.length === 0) return 0;
    const completedCount = todayTasks.filter((t) => t.completed).length;
    return Math.round((completedCount / todayTasks.length) * 100);
  };

  const getAccentColorForType = (type: string) => {
    switch (type.toUpperCase()) {
      case 'MEAL':
        return '#F59E0B';
      case 'WORKOUT':
        return '#10B981';
      case 'SUPPLEMENT':
        return '#8B5CF6';
      case 'REST':
        return '#3B82F6';
      default:
        return colors.accentSecondary;
    }
  };

  if (isLoading) {
    return (
      <View style={[styles.centerContainer, { backgroundColor: colors.background }]}>
        <ActivityIndicator size="large" color={colors.accent} />
        <Text style={[styles.loadingText, { color: colors.textSecondary }]}>Syncing schedule...</Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={styles.topHeader}>
        <View>
          <Text style={[styles.welcomeText, { color: colors.textSecondary }]}>Hello, {user?.profile?.name || 'Athlete'}</Text>
          <Text style={[styles.headerTitle, { color: colors.text }]}>Today's Routine</Text>
        </View>
        <TouchableOpacity style={[styles.logoutBtn, { borderColor: colors.cardBorder }]} onPress={logout}>
          <Text style={[styles.logoutBtnText, { color: colors.error }]}>Log Out</Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={handleRefresh} tintColor={colors.accent} />}
      >
        {activeRoutine ? (
          <>
            <GlassCard style={styles.progressCard}>
              <View style={styles.progressRow}>
                <View style={[styles.progressCircle, { borderColor: colors.accent }]}>
                  <Text style={[styles.progressPercentText, { color: colors.text }]}>
                    {getCompletionPercent()}%
                  </Text>
                  <Text style={[styles.progressCircleSub, { color: colors.textSecondary }]}>Done</Text>
                </View>
                <View style={styles.progressTextCol}>
                  <Text style={[styles.activeRoutineTitle, { color: colors.text }]}>
                    {activeRoutine.title}
                  </Text>
                  <Text style={[styles.progressSummaryText, { color: colors.textSecondary }]}>
                    Keep going! You have completed {todayTasks.filter((t) => t.completed).length} of{' '}
                    {todayTasks.length} tasks scheduled for today.
                  </Text>
                </View>
              </View>
            </GlassCard>

            {error && (
              <View style={[styles.errorBox, { borderColor: colors.error, backgroundColor: 'rgba(239,68,68,0.1)' }]}>
                <Text style={{ color: colors.error, fontSize: 12, fontWeight: 'bold' }}>{error}</Text>
              </View>
            )}

            <Text style={[styles.sectionTitle, { color: colors.text }]}>Schedule Timeline</Text>

            {todayTasks.length === 0 ? (
              <GlassCard style={styles.emptyCard}>
                <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                  Rest Day! No tasks scheduled for today. Enjoy your rest!
                </Text>
              </GlassCard>
            ) : (
              todayTasks.map((task) => {
                const typeAccent = getAccentColorForType(task.taskType);
                return (
                  <GlassCard
                    key={task.id}
                    style={[
                      styles.taskCard,
                      task.completed && { borderColor: colors.accent, backgroundColor: 'rgba(16, 185, 129, 0.05)' },
                      task.skipped && { opacity: 0.5 },
                    ]}
                  >
                    <View style={styles.taskHeader}>
                      <View style={[styles.timeBadge, { backgroundColor: typeAccent + '20' }]}>
                        <Text style={[styles.timeText, { color: typeAccent }]}>
                          {task.scheduledTime || '08:00'}
                        </Text>
                      </View>
                      <Text style={[styles.taskTypeTag, { color: typeAccent }]}>
                        {task.taskType.toUpperCase()}
                      </Text>
                    </View>

                    <Text
                      style={[
                        styles.taskDesc,
                        { color: colors.text },
                        task.completed && styles.lineThrough,
                      ]}
                    >
                      {task.description}
                    </Text>

                    {task.durationMinutes && (
                      <Text style={[styles.taskDuration, { color: colors.textSecondary }]}>
                        ⏱ Duration: {task.durationMinutes} mins
                      </Text>
                    )}

                    <View style={styles.actionRow}>
                      <TouchableOpacity
                        style={[
                          styles.actionBtn,
                          { borderColor: colors.cardBorder },
                          task.completed && { backgroundColor: colors.accent, borderColor: colors.accent },
                        ]}
                        onPress={() => toggleComplete(task.id)}
                      >
                        <Text style={[styles.actionBtnText, { color: task.completed ? '#fff' : colors.text }]}>
                          {task.completed ? '✓ Completed' : 'Mark Done'}
                        </Text>
                      </TouchableOpacity>

                      <TouchableOpacity
                        style={[
                          styles.actionBtn,
                          { borderColor: colors.cardBorder },
                          task.skipped && { backgroundColor: colors.textSecondary, borderColor: colors.textSecondary },
                        ]}
                        onPress={() => toggleSkip(task.id)}
                      >
                        <Text style={[styles.actionBtnText, { color: task.skipped ? '#fff' : colors.text }]}>
                          {task.skipped ? '⏭ Skipped' : 'Skip'}
                        </Text>
                      </TouchableOpacity>
                    </View>
                  </GlassCard>
                );
              })
            )}
          </>
        ) : (
          <View style={styles.noRoutineContainer}>
            <GlassCard style={styles.welcomeSetupCard}>
              <Text style={[styles.noRoutineTitle, { color: colors.text }]}>No Active Routine</Text>
              <Text style={[styles.noRoutineDesc, { color: colors.textSecondary }]}>
                To start tracking your daily diet plus workout routine, you need to generate one first! 
                You can do this by:
              </Text>
              <Text style={[styles.bulletItem, { color: colors.textSecondary }]}>
                1. Uploading a PDF/Excel coach routine in the **Upload** tab.
              </Text>
              <Text style={[styles.bulletItem, { color: colors.textSecondary }]}>
                2. Chatting directly with your **AI Coach** in the Chat tab to build one tailored to your goals.
              </Text>
            </GlassCard>
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  centerContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 14,
    fontWeight: '600',
  },
  topHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingTop: 64,
    paddingBottom: 16,
  },
  welcomeText: {
    fontSize: 12,
    fontWeight: '600',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    letterSpacing: -0.5,
  },
  logoutBtn: {
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  logoutBtnText: {
    fontSize: 11,
    fontWeight: 'bold',
  },
  scrollContent: {
    padding: 20,
    paddingBottom: 100,
  },
  progressCard: {
    marginBottom: 20,
  },
  progressRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 20,
  },
  progressCircle: {
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  progressPercentText: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  progressCircleSub: {
    fontSize: 8,
    fontWeight: '600',
  },
  progressTextCol: {
    flex: 1,
    gap: 4,
  },
  activeRoutineTitle: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  progressSummaryText: {
    fontSize: 12,
    lineHeight: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 12,
    marginTop: 10,
  },
  taskCard: {
    padding: 16,
    marginVertical: 6,
  },
  taskHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  timeBadge: {
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  timeText: {
    fontSize: 11,
    fontWeight: 'bold',
  },
  taskTypeTag: {
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 0.5,
  },
  taskDesc: {
    fontSize: 15,
    fontWeight: '700',
    marginTop: 12,
  },
  taskDuration: {
    fontSize: 11,
    marginTop: 6,
    fontWeight: '600',
  },
  lineThrough: {
    textDecorationLine: 'line-through',
    opacity: 0.7,
  },
  actionRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 16,
  },
  actionBtn: {
    flex: 1,
    height: 36,
    borderWidth: 1,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  actionBtnText: {
    fontSize: 12,
    fontWeight: 'bold',
  },
  noRoutineContainer: {
    marginTop: 48,
    alignItems: 'center',
  },
  welcomeSetupCard: {
    padding: 24,
    gap: 12,
  },
  noRoutineTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  noRoutineDesc: {
    fontSize: 13,
    lineHeight: 18,
    textAlign: 'center',
  },
  bulletItem: {
    fontSize: 12,
    lineHeight: 18,
    fontWeight: '600',
    paddingHorizontal: 8,
  },
  emptyCard: {
    padding: 24,
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 13,
    fontWeight: '600',
    textAlign: 'center',
  },
  errorBox: {
    borderWidth: 1,
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
  },
});
