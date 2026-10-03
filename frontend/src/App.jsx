import { Navigate, Route, Routes } from 'react-router-dom';
import { Protected } from './components.jsx';
import { PublicLayout, StaffLayout } from './layouts.jsx';
import Home from './pages/Home.jsx';
import RestaurantDetail from './pages/RestaurantDetail.jsx';
import BookingWizard from './pages/BookingWizard.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import OAuthCallback from './pages/OAuthCallback.jsx';
import MyReservations from './pages/MyReservations.jsx';
import ReservationDetail from './pages/ReservationDetail.jsx';
import StaffDashboard from './pages/StaffDashboard.jsx';
import StaffCalendar from './pages/StaffCalendar.jsx';
import StaffTables from './pages/StaffTables.jsx';
import StaffMenu from './pages/StaffMenu.jsx';
import AdminRestaurants from './pages/AdminRestaurants.jsx';
import AdminUsers from './pages/AdminUsers.jsx';
import AdminNotifications from './pages/AdminNotifications.jsx';

const STAFF = ['STAFF', 'ADMIN'];

export default function App() {
  return (
    <Routes>
      {/* auth screens: full-page split layout */}
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/oauth2/callback" element={<OAuthCallback />} />

      {/* public + customer pages */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/restaurants/:id" element={<RestaurantDetail />} />
        <Route path="/restaurants/:id/book" element={<Protected roles={['CUSTOMER']}><BookingWizard /></Protected>} />
        <Route path="/reservations" element={<Protected roles={['CUSTOMER']}><MyReservations /></Protected>} />
        <Route path="/reservations/:id" element={<Protected roles={['CUSTOMER']}><ReservationDetail /></Protected>} />
      </Route>

      {/* staff + admin area (sidebar layout) */}
      <Route element={<Protected roles={STAFF}><StaffLayout /></Protected>}>
        <Route path="/staff" element={<StaffDashboard />} />
        <Route path="/staff/reservations" element={<StaffCalendar />} />
        <Route path="/staff/reservations/:id" element={<ReservationDetail />} />
        <Route path="/staff/tables" element={<StaffTables />} />
        <Route path="/staff/menu" element={<StaffMenu />} />
      </Route>
      <Route element={<Protected roles={['ADMIN']}><StaffLayout /></Protected>}>
        <Route path="/admin/restaurants" element={<AdminRestaurants />} />
        <Route path="/admin/users" element={<AdminUsers />} />
        <Route path="/admin/notifications" element={<AdminNotifications />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
