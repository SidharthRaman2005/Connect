import { Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import About from "./pages/About";
import Dashboard from "./pages/Dashboard";
import Onboarding from "./pages/Onboarding";
import Profile from "./pages/Profile";
import StudentProfile from "./pages/StudentProfile";
import Search from "./pages/Search";
import Discover from "./pages/Discover";
import Connections from "./pages/Connections";
import MyHighlights from "./pages/MyHighlights";
import Collabs from "./pages/Collabs";
import AddCollab from "./pages/AddCollab";
import Chat from "./pages/Chat";
import ProtectedRoute from "./components/ProtectedRoute";
import AppShell from "./components/AppShell";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/about" element={<About />} />
      <Route
        path="/onboarding"
        element={
          <ProtectedRoute>
            <Onboarding />
          </ProtectedRoute>
        }
      />
      <Route element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/students/:id" element={<StudentProfile />} />
        <Route path="/search" element={<Search />} />
        <Route path="/discover" element={<Discover />} />
        <Route path="/connections" element={<Connections />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/highlights" element={<MyHighlights />} />
        <Route path="/collabs" element={<Collabs />} />
        <Route path="/collabs/add" element={<AddCollab />} />
      </Route>
    </Routes>
  );
}

export default App;
