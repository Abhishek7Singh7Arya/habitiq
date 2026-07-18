import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  ActivityIndicator,
  useColorScheme,
  Alert,
  RefreshControl,
} from 'react-native';
import { useAuth } from '../context/AuthContext';
import { Colors } from '../constants/theme';
import { GlassCard } from '../components/GlassCard';
import axios from 'axios';

interface Goal {
  id: string;
  goalType: string;
  targetValue: number;
  currentValue?: number;
  unit: string;
  targetDate?: string;
  status: string;
}

interface ProgressLog {
  id: string;
  loggedValue: number;
  unit: string;
  notes?: string;
  loggedAt: string;
}

export default function ProgressDashboard() {
  const { token, user, apiBaseUrl } = useAuth();
  const [goals, setGoals] = useState<Goal[]>([]);
  const [activeGoal, setActiveGoal] = useState<Goal | null>(null);
  const [logs, setLogs] = useState<ProgressLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const [showGoalForm, setShowGoalForm] = useState(false);
  const [goalType, setGoalType] = useState('WEIGHT_LOSS');
  const [targetValue, setTargetValue] = useState('');
  const [currentValue, setCurrentValue] = useState('');
  const [unit, setUnit] = useState('kg');
  const [targetDate, setTargetDate] = useState('2026-12-31');

  const [showLogForm, setShowLogForm] = useState(false);
  const [logValue, setLogValue] = useState('');
  const [logNote, setLogNote] = useState('');

  const colorScheme = useColorScheme() ?? 'dark';
  const colors = Colors[colorScheme];

  const fetchProgressData = async () => {
    if (!token || !user) return;
    try {
      const headers = {
        Authorization: `Bearer ${token}`,
        'X-User-Id': user.id,
      };

      const goalsResponse = await axios.get(`${apiBaseUrl}/api/progress/goals`, { headers });
      if (goalsResponse.data?.success) {
        const fetchedGoals: Goal[] = goalsResponse.data.data;
        setGoals(fetchedGoals);
        if (fetchedGoals.length > 0) {
          setActiveGoal(fetchedGoals[0]);
          fetchLogsForGoal(fetchedGoals[0].id);
        }
      }
    } catch (e: any) {
      console.error('Failed to fetch progress data', e);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  const fetchLogsForGoal = async (goalId: string) => {
    setLogs([
      {
        id: '1',
        loggedValue: activeGoal?.currentValue || 83,
        unit: activeGoal?.unit || 'kg',
        notes: 'Morning run check',
        loggedAt: new Date().toISOString(),
      },
    ]);
  };

  useEffect(() => {
    fetchProgressData();
  }, []);

  const handleRefresh = () => {
    setIsRefreshing(true);
    fetchProgressData();
  };

  const handleCreateGoal = async () => {
    if (!targetValue || !unit) {
      Alert.alert('Error', 'Target value and unit are required.');
      return;
    }

    try {
      const response = await axios.post(
        `${apiBaseUrl}/api/progress/goals`,
        {
          goalType,
          targetValue: parseFloat(targetValue),
          currentValue: currentValue ? parseFloat(currentValue) : undefined,
          unit,
          targetDate,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-User-Id': user?.id,
          },
        }
      );

      if (response.data?.success) {
        Alert.alert('Success', 'Goal created successfully!');
        setShowGoalForm(false);
        setTargetValue('');
        setCurrentValue('');
        fetchProgressData();
      }
    } catch (e: any) {
      Alert.alert('Failed', e.response?.data?.message || e.message || 'Failed to create goal.');
    }
  };

  const handleLogProgress = async () => {
    if (!activeGoal || !logValue) {
      Alert.alert('Error', 'Please enter a logging value.');
      return;
    }

    try {
      const response = await axios.post(
        `${apiBaseUrl}/api/progress/goals/${activeGoal.id}/log`,
        {
          value: parseFloat(logValue),
          unit: activeGoal.unit,
          notes: logNote,
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'X-User-Id': user?.id,
          },
        }
      );

      if (response.data?.success) {
        Alert.alert('Success', 'Progress logged successfully!');
        setShowLogForm(false);
        setLogValue('');
        setLogNote('');
        fetchProgressData();
      }
    } catch (e: any) {
      Alert.alert('Failed', e.response?.data?.message || e.message || 'Failed to log progress.');
    }
  };

  if (isLoading) {
    return (
      <View style={[styles.centerContainer, { backgroundColor: colors.background }]}>
        <ActivityIndicator size="large" color={colors.accent} />
        <Text style={[styles.loadingText, { color: colors.textSecondary }]}>Loading goals...</Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: colors.text }]}>Goal Progress</Text>
        <Text style={[styles.headerSubtitle, { color: colors.textSecondary }]}>
          Track your fitness milestones and log your daily metrics.
        </Text>
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={handleRefresh} tintColor={colors.accent} />}
      >
        {goals.length === 0 ? (
          <GlassCard style={styles.noGoalsCard}>
            <Text style={[styles.noGoalsTitle, { color: colors.text }]}>No Goals Created</Text>
            <Text style={[styles.noGoalsDesc, { color: colors.textSecondary }]}>
              Create a goal to begin tracking weight loss, muscle gain, or stamina achievements!
            </Text>
            <TouchableOpacity
              style={[styles.primaryBtn, { backgroundColor: colors.accent }]}
              onPress={() => setShowGoalForm(true)}
            >
              <Text style={styles.btnText}>Create New Goal</Text>
            </TouchableOpacity>
          </GlassCard>
        ) : (
          <>
            {activeGoal && (
              <GlassCard style={styles.metricsCard}>
                <View style={styles.goalHeader}>
                  <Text style={[styles.goalTypeTag, { color: colors.accent }]}>
                    {activeGoal.goalType.replace('_', ' ')}
                  </Text>
                  <Text style={[styles.goalTargetDate, { color: colors.textSecondary }]}>
                    Target: {activeGoal.targetDate || 'No date'}
                  </Text>
                </View>

                <View style={styles.metricsRow}>
                  <View style={styles.metricItem}>
                    <Text style={[styles.metricLabel, { color: colors.textSecondary }]}>Current</Text>
                    <Text style={[styles.metricValue, { color: colors.text }]}>
                      {activeGoal.currentValue || '-'} {activeGoal.unit}
                    </Text>
                  </View>
                  <View style={styles.metricDivider} />
                  <View style={styles.metricItem}>
                    <Text style={[styles.metricLabel, { color: colors.textSecondary }]}>Target</Text>
                    <Text style={[styles.metricValue, { color: colors.text }]}>
                      {activeGoal.targetValue} {activeGoal.unit}
                    </Text>
                  </View>
                </View>

                <TouchableOpacity
                  style={[styles.primaryBtn, { backgroundColor: colors.accent, marginTop: 16 }]}
                  onPress={() => setShowLogForm(true)}
                >
                  <Text style={styles.btnText}>Log Today's Progress</Text>
                </TouchableOpacity>
              </GlassCard>
            )}

            <View style={styles.sectionHeader}>
              <Text style={[styles.sectionTitle, { color: colors.text }]}>My Goals</Text>
              <TouchableOpacity onPress={() => setShowGoalForm(true)}>
                <Text style={{ color: colors.accent, fontWeight: 'bold', fontSize: 13 }}>+ Add Goal</Text>
              </TouchableOpacity>
            </View>

            {goals.map((g) => (
              <GlassCard
                key={g.id}
                style={[
                  styles.goalListItem,
                  activeGoal?.id === g.id && { borderColor: colors.accent },
                ]}
                onPress={() => {
                  setActiveGoal(g);
                  fetchLogsForGoal(g.id);
                }}
              >
                <View style={styles.goalListRow}>
                  <View>
                    <Text style={[styles.goalListTitle, { color: colors.text }]}>
                      {g.goalType.replace('_', ' ')}
                    </Text>
                    <Text style={[styles.goalListSub, { color: colors.textSecondary }]}>
                      Current: {g.currentValue || '-'} | Target: {g.targetValue} {g.unit}
                    </Text>
                  </View>
                  <Text style={[styles.activeIndicator, { color: colors.accent }]}>
                    {activeGoal?.id === g.id ? '● Active' : 'Select'}
                  </Text>
                </View>
              </GlassCard>
            ))}
          </>
        )}

        {showGoalForm && (
          <GlassCard style={styles.formCard}>
            <Text style={[styles.formTitle, { color: colors.text }]}>Create Milestone Goal</Text>
            
            <Text style={[styles.label, { color: colors.textSecondary }]}>Goal Type</Text>
            <View style={styles.row}>
              {['WEIGHT_LOSS', 'MUSCLE_GAIN', 'STAMINA'].map((type) => (
                <TouchableOpacity
                  key={type}
                  style={[
                    styles.chip,
                    {
                      borderColor: colors.cardBorder,
                      backgroundColor: goalType === type ? colors.backgroundSelected : 'transparent',
                    },
                  ]}
                  onPress={() => setGoalType(type)}
                >
                  <Text style={{ color: colors.text, fontSize: 10, fontWeight: 'bold' }}>
                    {type.replace('_', ' ')}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <View style={styles.row}>
              <View style={styles.flexHalf}>
                <Text style={[styles.label, { color: colors.textSecondary }]}>Current Value</Text>
                <TextInput
                  style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                  placeholder="85"
                  placeholderTextColor={colors.textSecondary}
                  value={currentValue}
                  onChangeText={setCurrentValue}
                  keyboardType="numeric"
                />
              </View>
              <View style={styles.flexHalf}>
                <Text style={[styles.label, { color: colors.textSecondary }]}>Target Value</Text>
                <TextInput
                  style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                  placeholder="75"
                  placeholderTextColor={colors.textSecondary}
                  value={targetValue}
                  onChangeText={setTargetValue}
                  keyboardType="numeric"
                />
              </View>
            </View>

            <View style={styles.row}>
              <View style={styles.flexHalf}>
                <Text style={[styles.label, { color: colors.textSecondary }]}>Unit</Text>
                <TextInput
                  style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                  placeholder="kg / lbs"
                  placeholderTextColor={colors.textSecondary}
                  value={unit}
                  onChangeText={setUnit}
                />
              </View>
              <View style={styles.flexHalf}>
                <Text style={[styles.label, { color: colors.textSecondary }]}>Target Date</Text>
                <TextInput
                  style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                  placeholder="2026-12-31"
                  placeholderTextColor={colors.textSecondary}
                  value={targetDate}
                  onChangeText={setTargetDate}
                />
              </View>
            </View>

            <View style={styles.formActionRow}>
              <TouchableOpacity
                style={[styles.actionBtn, { backgroundColor: colors.accent }]}
                onPress={handleCreateGoal}
              >
                <Text style={styles.btnText}>Create</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.actionBtn, { borderColor: colors.cardBorder }]}
                onPress={() => setShowGoalForm(false)}
              >
                <Text style={{ color: colors.text, fontWeight: 'bold' }}>Cancel</Text>
              </TouchableOpacity>
            </View>
          </GlassCard>
        )}

        {showLogForm && (
          <GlassCard style={styles.formCard}>
            <Text style={[styles.formTitle, { color: colors.text }]}>Log Progress Metric</Text>

            <Text style={[styles.label, { color: colors.textSecondary }]}>Today's Value ({activeGoal?.unit})</Text>
            <TextInput
              style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
              placeholder="e.g. 83.5"
              placeholderTextColor={colors.textSecondary}
              value={logValue}
              onChangeText={setLogValue}
              keyboardType="numeric"
            />

            <Text style={[styles.label, { color: colors.textSecondary }]}>Notes / Achievements</Text>
            <TextInput
              style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
              placeholder="Morning fast run / feeling energetic"
              placeholderTextColor={colors.textSecondary}
              value={logNote}
              onChangeText={setLogNote}
            />

            <View style={styles.formActionRow}>
              <TouchableOpacity
                style={[styles.actionBtn, { backgroundColor: colors.accent }]}
                onPress={handleLogProgress}
              >
                <Text style={styles.btnText}>Log Value</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.actionBtn, { borderColor: colors.cardBorder }]}
                onPress={() => setShowLogForm(false)}
              >
                <Text style={{ color: colors.text, fontWeight: 'bold' }}>Cancel</Text>
              </TouchableOpacity>
            </View>
          </GlassCard>
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
    paddingBottom: 100,
    gap: 16,
  },
  noGoalsCard: {
    padding: 24,
    alignItems: 'center',
    gap: 12,
  },
  noGoalsTitle: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  noGoalsDesc: {
    fontSize: 12,
    textAlign: 'center',
    lineHeight: 16,
  },
  primaryBtn: {
    height: 40,
    borderRadius: 8,
    paddingHorizontal: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  metricsCard: {
    padding: 20,
  },
  goalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  goalTypeTag: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  goalTargetDate: {
    fontSize: 11,
    fontWeight: '600',
  },
  metricsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    marginVertical: 12,
  },
  metricItem: {
    alignItems: 'center',
    gap: 4,
  },
  metricLabel: {
    fontSize: 12,
    fontWeight: '600',
  },
  metricValue: {
    fontSize: 22,
    fontWeight: '900',
  },
  metricDivider: {
    width: 1,
    height: 48,
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 16,
    marginBottom: 8,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  goalListItem: {
    padding: 16,
  },
  goalListRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  goalListTitle: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  goalListSub: {
    fontSize: 11,
    marginTop: 2,
    fontWeight: '600',
  },
  activeIndicator: {
    fontSize: 12,
    fontWeight: 'bold',
  },
  formCard: {
    padding: 20,
    marginTop: 12,
  },
  formTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 12,
  },
  label: {
    fontSize: 11,
    fontWeight: '600',
    marginBottom: 6,
    marginTop: 10,
  },
  input: {
    height: 40,
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 12,
    fontSize: 13,
  },
  row: {
    flexDirection: 'row',
    gap: 12,
    alignItems: 'center',
    marginVertical: 4,
  },
  flexHalf: {
    flex: 1,
  },
  chip: {
    flex: 1,
    height: 32,
    borderWidth: 1,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  formActionRow: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 20,
  },
  actionBtn: {
    flex: 1,
    height: 40,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
