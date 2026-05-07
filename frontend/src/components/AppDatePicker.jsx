import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';

function parseIsoDate(value) {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function formatIsoDate(date) {
  if (!date) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function AppDatePicker({ label, value, onChange, className = '' }) {
  return (
    <label className={className}>
      {label}
      <DatePicker
        selected={parseIsoDate(value)}
        onChange={(date) => onChange(formatIsoDate(date))}
        dateFormat="yyyy-MM-dd"
        placeholderText="YYYY-MM-DD"
        className="app-date-picker-input"
      />
    </label>
  );
}

export default AppDatePicker;
