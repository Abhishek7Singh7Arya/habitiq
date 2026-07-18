export const Colors = {
  light: {
    background: '#F9FAFB',
    text: '#111827',
    textSecondary: '#6B7280',
    accent: '#10B981',
    accentSecondary: '#8B5CF6',
    cardBackground: 'rgba(255, 255, 255, 0.7)',
    cardBorder: 'rgba(243, 244, 246, 1)',
    backgroundSelected: 'rgba(16, 185, 129, 0.1)',
    error: '#EF4444',
  },
  dark: {
    background: '#090d16',
    text: '#FFFFFF',
    textSecondary: '#9CA3AF',
    accent: '#10B981',
    accentSecondary: '#8B5CF6',
    cardBackground: 'rgba(15, 23, 42, 0.45)',
    cardBorder: 'rgba(255, 255, 255, 0.08)',
    backgroundSelected: 'rgba(16, 185, 129, 0.15)',
    error: '#EF4444',
  },
};
export type ThemeColors = typeof Colors.dark;
