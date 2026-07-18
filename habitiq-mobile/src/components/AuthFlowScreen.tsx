import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  useColorScheme,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useAuth } from '../context/AuthContext';
import { Colors } from '../constants/theme';
import { GlassCard } from './GlassCard';

export const AuthFlowScreen: React.FC = () => {
  const [screen, setScreen] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [age, setAge] = useState('');
  const [gender, setGender] = useState<'MALE' | 'FEMALE' | 'OTHER' | ''>('');
  const [weight, setWeight] = useState('');
  const [height, setHeight] = useState('');
  const [goal, setGoal] = useState<'WEIGHT_LOSS' | 'WEIGHT_GAIN' | 'MUSCLE_BUILDING' | 'STAMINA_INCREASE' | 'GENERAL_FITNESS' | 'FLEXIBILITY' | ''>('');
  const [activity, setActivity] = useState<'SEDENTARY' | 'LIGHTLY_ACTIVE' | 'MODERATELY_ACTIVE' | 'VERY_ACTIVE' | 'EXTREMELY_ACTIVE' | ''>('');
  const [healthConditions, setHealthConditions] = useState('');
  const [dietaryPreferences, setDietaryPreferences] = useState('');

  const { login, register, isLoading } = useAuth();
  const colorScheme = useColorScheme() ?? 'dark';
  const colors = Colors[colorScheme];

  const handleLogin = async () => {
    if (!email || !password) {
      setErrorMsg('Please enter both email and password.');
      return;
    }
    setErrorMsg(null);
    try {
      await login(email, password);
    } catch (e: any) {
      setErrorMsg(e.message || 'Login failed.');
    }
  };

  const validateForm = (): boolean => {
    if (!name.trim()) {
      setErrorMsg('Full Name is required.');
      return false;
    }
    if (!email.trim() || !email.includes('@')) {
      setErrorMsg('A valid Email address is required.');
      return false;
    }
    const phoneRegex = /^\+[1-9]\d{1,14}$/;
    if (!phone.trim() || !phoneRegex.test(phone)) {
      setErrorMsg('WhatsApp Number is required in E.164 format, e.g. +919876543210 (must start with + and country code).');
      return false;
    }
    if (!password || password.length < 8) {
      setErrorMsg('Password must be at least 8 characters long.');
      return false;
    }
    
    if (age.trim()) {
      const parsedAge = parseInt(age, 10);
      if (isNaN(parsedAge) || parsedAge <= 0) {
        setErrorMsg('Age must be a valid positive number.');
        return false;
      }
    }
    if (weight.trim()) {
      const parsedWeight = parseFloat(weight);
      if (isNaN(parsedWeight) || parsedWeight <= 0) {
        setErrorMsg('Weight must be a valid positive number.');
        return false;
      }
    }
    if (height.trim()) {
      const parsedHeight = parseFloat(height);
      if (isNaN(parsedHeight) || parsedHeight <= 0) {
        setErrorMsg('Height must be a valid positive number.');
        return false;
      }
    }

    setErrorMsg(null);
    return true;
  };

  const executeRegistration = async () => {
    if (!validateForm()) return;
    setErrorMsg(null);
    try {
      await register({
        name: name.trim(),
        email: email.trim().toLowerCase(),
        phone: phone.trim(),
        password,
        age: age ? parseInt(age, 10) : undefined,
        gender: gender || undefined,
        weightKg: weight ? parseFloat(weight) : undefined,
        heightCm: height ? parseFloat(height) : undefined,
        fitnessGoal: goal || undefined,
        activityLevel: activity || undefined,
        healthConditions: healthConditions.trim() || undefined,
        dietaryPreferences: dietaryPreferences.trim() || undefined,
      });
    } catch (e: any) {
      setErrorMsg(e.message || 'Registration failed.');
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={[styles.container, { backgroundColor: colors.background }]}
    >
      <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
        <View style={styles.header}>
          <Text style={[styles.title, { color: colors.accent }]}>HabitIQ</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            AI-Powered Fitness & Diet Coaching
          </Text>
        </View>

        <GlassCard style={styles.formCard}>
          {errorMsg && (
            <View style={[styles.errorContainer, { backgroundColor: 'rgba(239, 68, 68, 0.15)', borderColor: colors.error }]}>
              <Text style={[styles.errorText, { color: colors.error }]}>{errorMsg}</Text>
            </View>
          )}

          {screen === 'login' && (
            <>
              <Text style={[styles.formTitle, { color: colors.text }]}>Welcome Back</Text>
              
              <Text style={[styles.label, { color: colors.textSecondary }]}>Email</Text>
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                placeholder="Enter your email"
                placeholderTextColor={colors.textSecondary}
                value={email}
                onChangeText={setEmail}
                keyboardType="email-address"
                autoCapitalize="none"
              />

              <Text style={[styles.label, { color: colors.textSecondary }]}>Password</Text>
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                placeholder="Enter your password"
                placeholderTextColor={colors.textSecondary}
                value={password}
                onChangeText={setPassword}
                secureTextEntry
                autoCapitalize="none"
              />

              <TouchableOpacity
                style={[styles.button, { backgroundColor: colors.accent }]}
                onPress={handleLogin}
                disabled={isLoading}
              >
                {isLoading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Log In</Text>}
              </TouchableOpacity>

              <TouchableOpacity style={styles.switchButton} onPress={() => { setScreen('register'); setErrorMsg(null); }}>
                <Text style={[styles.switchText, { color: colors.accent }]}>Don't have an account? Sign Up</Text>
              </TouchableOpacity>
            </>
          )}

          {screen === 'register' && (
            <>
              <View style={styles.formHeader}>
                <Text style={[styles.formTitle, { color: colors.text }]}>Create Account</Text>
              </View>

              <Text style={[styles.formSubtitle, { color: colors.textSecondary }]}>
                Please fill in the form below to set up your profile. Custom coaching awaits you!
              </Text>

              <View style={styles.sectionHeader}>
                <Text style={[styles.sectionTitle, { color: colors.accent }]}>1. Account Details</Text>
              </View>

              <View style={styles.labelRow}>
                <Text style={[styles.fieldLabel, { color: colors.text }]}>Full Name</Text>
                <View style={[styles.tag, { backgroundColor: colors.accent + '20' }]}>
                  <Text style={[styles.tagText, { color: colors.accent }]}>Required</Text>
                </View>
              </View>
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                placeholder="John Doe"
                placeholderTextColor={colors.textSecondary}
                value={name}
                onChangeText={setName}
                autoCapitalize="words"
              />

              <View style={styles.labelRow}>
                <Text style={[styles.fieldLabel, { color: colors.text }]}>Email Address</Text>
                <View style={[styles.tag, { backgroundColor: colors.accent + '20' }]}>
                  <Text style={[styles.tagText, { color: colors.accent }]}>Required</Text>
                </View>
              </View>
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                placeholder="you@example.com"
                placeholderTextColor={colors.textSecondary}
                value={email}
                onChangeText={setEmail}
                keyboardType="email-address"
                autoCapitalize="none"
              />

              <View style={styles.labelRow}>
                <Text style={[styles.fieldLabel, { color: colors.text }]}>WhatsApp Phone Number</Text>
                <View style={[styles.tag, { backgroundColor: colors.accent + '20' }]}>
                  <Text style={[styles.tagText, { color: colors.accent }]}>Required</Text>
                </View>
              </View>
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                placeholder="+919876543210"
                placeholderTextColor={colors.textSecondary}
                value={phone}
                onChangeText={setPhone}
                keyboardType="phone-pad"
                autoCapitalize="none"
              />
              <Text style={styles.inputHint}>Format: +[Country Code][Number], e.g., +919876543210</Text>

              <View style={styles.labelRow}>
                <Text style={[styles.fieldLabel, { color: colors.text }]}>Password</Text>
                <View style={[styles.tag, { backgroundColor: colors.accent + '20' }]}>
                  <Text style={[styles.tagText, { color: colors.accent }]}>Required</Text>
                </View>
              </View>
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                placeholder="Minimum 8 characters"
                placeholderTextColor={colors.textSecondary}
                value={password}
                onChangeText={setPassword}
                secureTextEntry
                autoCapitalize="none"
              />

              <View style={styles.sectionDivider} />

              <View style={styles.sectionHeader}>
                <Text style={[styles.sectionTitle, { color: colors.accentSecondary }]}>2. Personal Metrics</Text>
              </View>

              <View style={styles.row}>
                <View style={styles.flexHalf}>
                  <View style={styles.labelRow}>
                    <Text style={[styles.fieldLabel, { color: colors.text }]}>Age (Years)</Text>
                    <View style={[styles.tag, { backgroundColor: 'rgba(255, 255, 255, 0.08)' }]}>
                      <Text style={[styles.tagText, { color: colors.textSecondary }]}>Optional</Text>
                    </View>
                  </View>
                  <TextInput
                    style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                    placeholder="25"
                    placeholderTextColor={colors.textSecondary}
                    value={age}
                    onChangeText={setAge}
                    keyboardType="numeric"
                  />
                </View>
                <View style={styles.flexHalf}>
                  <View style={styles.labelRow}>
                    <Text style={[styles.fieldLabel, { color: colors.text }]}>Gender</Text>
                    <View style={[styles.tag, { backgroundColor: 'rgba(255, 255, 255, 0.08)' }]}>
                      <Text style={[styles.tagText, { color: colors.textSecondary }]}>Optional</Text>
                    </View>
                  </View>
                  <View style={styles.genderRow}>
                    {(['MALE', 'FEMALE', 'OTHER'] as const).map((g) => (
                      <TouchableOpacity
                        key={g}
                        style={[
                          styles.choiceChip,
                          { borderColor: colors.cardBorder, backgroundColor: gender === g ? colors.backgroundSelected : 'transparent' },
                        ]}
                        onPress={() => setGender(gender === g ? '' : g)}
                      >
                        <Text style={{ color: colors.text, fontSize: 10, fontWeight: 'bold' }}>
                          {g}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                </View>
              </View>

              <View style={styles.row}>
                <View style={styles.flexHalf}>
                  <View style={styles.labelRow}>
                    <Text style={[styles.fieldLabel, { color: colors.text }]}>Weight (kg)</Text>
                    <View style={[styles.tag, { backgroundColor: 'rgba(255, 255, 255, 0.08)' }]}>
                      <Text style={[styles.tagText, { color: colors.textSecondary }]}>Optional</Text>
                    </View>
                  </View>
                  <TextInput
                    style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                    placeholder="70"
                    placeholderTextColor={colors.textSecondary}
                    value={weight}
                    onChangeText={setWeight}
                    keyboardType="numeric"
                  />
                </View>
                <View style={styles.flexHalf}>
                  <View style={styles.labelRow}>
                    <Text style={[styles.fieldLabel, { color: colors.text }]}>Height (cm)</Text>
                    <View style={[styles.tag, { backgroundColor: 'rgba(255, 255, 255, 0.08)' }]}>
                      <Text style={[styles.tagText, { color: colors.textSecondary }]}>Optional</Text>
                    </View>
                  </View>
                  <TextInput
                    style={[styles.input, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                    placeholder="175"
                    placeholderTextColor={colors.textSecondary}
                    value={height}
                    onChangeText={setHeight}
                    keyboardType="numeric"
                  />
                </View>
              </View>

              <View style={styles.sectionDivider} />

              <View style={styles.sectionHeader}>
                <Text style={[styles.sectionTitle, { color: colors.accentSecondary }]}>3. Goals & Activity</Text>
              </View>

              <View style={styles.labelRow}>
                <Text style={[styles.fieldLabel, { color: colors.text }]}>Primary Fitness Goal</Text>
                <View style={[styles.tag, { backgroundColor: 'rgba(255, 255, 255, 0.08)' }]}>
                  <Text style={[styles.tagText, { color: colors.textSecondary }]}>Optional</Text>
                </View>
              </View>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipScroll}>
                {([
                  'WEIGHT_LOSS',
                  'WEIGHT_GAIN',
                  'MUSCLE_BUILDING',
                  'STAMINA_INCREASE',
                  'GENERAL_FITNESS',
                  'FLEXIBILITY',
                ] as const).map((g) => (
                  <TouchableOpacity
                    key={g}
                    style={[
                      styles.goalChip,
                      { borderColor: colors.cardBorder, backgroundColor: goal === g ? colors.backgroundSelected : 'transparent' },
                    ]}
                    onPress={() => setGoal(goal === g ? '' : g)}
                  >
                    <Text style={{ color: colors.text, fontSize: 11, fontWeight: 'bold' }}>
                      {g.replace('_', ' ')}
                    </Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>

              <View style={styles.labelRow}>
                <Text style={[styles.fieldLabel, { color: colors.text }]}>Daily Activity Level</Text>
                <View style={[styles.tag, { backgroundColor: 'rgba(255, 255, 255, 0.08)' }]}>
                  <Text style={[styles.tagText, { color: colors.textSecondary }]}>Optional</Text>
                </View>
              </View>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipScroll}>
                {([
                  'SEDENTARY',
                  'LIGHTLY_ACTIVE',
                  'MODERATELY_ACTIVE',
                  'VERY_ACTIVE',
                  'EXTREMELY_ACTIVE',
                ] as const).map((act) => (
                  <TouchableOpacity
                    key={act}
                    style={[
                      styles.goalChip,
                      { borderColor: colors.cardBorder, backgroundColor: activity === act ? colors.backgroundSelected : 'transparent' },
                    ]}
                    onPress={() => setActivity(activity === act ? '' : act)}
                  >
                    <Text style={{ color: colors.text, fontSize: 11, fontWeight: 'bold' }}>
                      {act.replace('_', ' ')}
                    </Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>

              <View style={styles.sectionDivider} />

              <View style={styles.sectionHeader}>
                <Text style={[styles.sectionTitle, { color: colors.accentSecondary }]}>4. Lifestyle & Health</Text>
              </View>

              <View style={styles.labelRow}>
                <Text style={[styles.fieldLabel, { color: colors.text }]}>Health Conditions</Text>
                <View style={[styles.tag, { backgroundColor: 'rgba(255, 255, 255, 0.08)' }]}>
                  <Text style={[styles.tagText, { color: colors.textSecondary }]}>Optional</Text>
                </View>
              </View>
              <TextInput
                style={[styles.inputLarge, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                placeholder="Asthma, back pain, knee injuries, etc. (Leave blank if none)"
                placeholderTextColor={colors.textSecondary}
                value={healthConditions}
                onChangeText={setHealthConditions}
                multiline
                numberOfLines={3}
              />

              <View style={styles.labelRow}>
                <Text style={[styles.fieldLabel, { color: colors.text }]}>Dietary Preferences</Text>
                <View style={[styles.tag, { backgroundColor: 'rgba(255, 255, 255, 0.08)' }]}>
                  <Text style={[styles.tagText, { color: colors.textSecondary }]}>Optional</Text>
                </View>
              </View>
              <TextInput
                style={[styles.inputLarge, { color: colors.text, borderColor: colors.cardBorder, backgroundColor: colors.background }]}
                placeholder="Keto, Vegan, Vegetarian, High Protein, Allergies (Leave blank if none)"
                placeholderTextColor={colors.textSecondary}
                value={dietaryPreferences}
                onChangeText={setDietaryPreferences}
                multiline
                numberOfLines={3}
              />

              <TouchableOpacity
                style={[styles.button, { backgroundColor: colors.accent, marginTop: 32 }]}
                onPress={executeRegistration}
                disabled={isLoading}
              >
                {isLoading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Submit & Register</Text>}
              </TouchableOpacity>

              <TouchableOpacity style={styles.switchButton} onPress={() => { setScreen('login'); setErrorMsg(null); }}>
                <Text style={[styles.switchText, { color: colors.accent }]}>Already have an account? Log In</Text>
              </TouchableOpacity>
            </>
          )}
        </GlassCard>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  scrollContent: {
    padding: 20,
    justifyContent: 'center',
    minHeight: '100%',
  },
  header: {
    alignItems: 'center',
    marginBottom: 24,
    marginTop: 32,
  },
  title: {
    fontSize: 36,
    fontWeight: '900',
    letterSpacing: -1,
  },
  subtitle: {
    fontSize: 13,
    marginTop: 4,
    fontWeight: '500',
  },
  formCard: {
    padding: 20,
  },
  formHeader: {
    marginBottom: 12,
  },
  formTitle: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  formSubtitle: {
    fontSize: 12,
    marginBottom: 12,
    lineHeight: 16,
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 6,
    marginTop: 12,
  },
  labelRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 14,
    marginBottom: 6,
  },
  fieldLabel: {
    fontSize: 12,
    fontWeight: '600',
  },
  tag: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tagText: {
    fontSize: 9,
    fontWeight: '800',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  sectionHeader: {
    marginTop: 12,
    marginBottom: 4,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0.5,
    textTransform: 'uppercase',
  },
  sectionDivider: {
    height: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    marginVertical: 20,
  },
  input: {
    height: 46,
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 12,
    fontSize: 14,
  },
  inputLarge: {
    minHeight: 64,
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingTop: 8,
    fontSize: 14,
    textAlignVertical: 'top',
  },
  inputHint: {
    fontSize: 10,
    color: '#9CA3AF',
    marginTop: 4,
    paddingLeft: 2,
  },
  row: {
    flexDirection: 'row',
    gap: 12,
    alignItems: 'center',
    marginVertical: 4,
  },
  genderRow: {
    flexDirection: 'row',
    gap: 4,
    height: 46,
    alignItems: 'center',
  },
  flexHalf: {
    flex: 1,
  },
  choiceChip: {
    flex: 1,
    height: 46,
    borderWidth: 1,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  goalChip: {
    height: 38,
    borderWidth: 1,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
    marginRight: 8,
  },
  chipScroll: {
    paddingVertical: 4,
  },
  button: {
    height: 46,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#10B981',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 2,
    marginTop: 20,
  },
  buttonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: 'bold',
  },
  switchButton: {
    alignItems: 'center',
    marginTop: 16,
  },
  switchText: {
    fontSize: 13,
    fontWeight: '600',
  },
  errorContainer: {
    borderWidth: 1,
    borderRadius: 8,
    padding: 10,
    marginBottom: 16,
  },
  errorText: {
    fontSize: 12,
    fontWeight: '600',
  },
});
