import { Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import About from "./pages/About";
import Dashboard from "./pages/Dashboard";
import Onboarding from "./pages/Onboarding";
import Profile from "./pages/Profile";
import StudentProfile from "./pages/StudentProfile";
import Search from "./pages/Search";
import Connections from "./pages/Connections";
import MyHighlights from "./pages/MyHighlights";
import Collabs from "./pages/Collabs";
import AddCollab from "./pages/AddCollab";
import Chat from "./pages/Chat";
import ProtectedRoute from "./components/ProtectedRoute";

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
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <Profile />
          </ProtectedRoute>
        }
      />
      <Route
        path="/students/:id"
        element={
          <ProtectedRoute>
            <StudentProfile />
          </ProtectedRoute>
        }
      />
      <Route
        path="/search"
        element={
          <ProtectedRoute>
            <Search />
          </ProtectedRoute>
        }
      />
      <Route
        path="/connections"
        element={
          <ProtectedRoute>
            <Connections />
          </ProtectedRoute>
        }
      />
      <Route
        path="/chat"
        element={
          <ProtectedRoute>
            <Chat />
          </ProtectedRoute>
        }
      />
      <Route
        path="/highlights"
        element={
          <ProtectedRoute>
            <MyHighlights />
          </ProtectedRoute>
        }
      />
      <Route
        path="/collabs"
        element={
          <ProtectedRoute>
            <Collabs />
          </ProtectedRoute>
        }
      />
      <Route
        path="/collabs/add"
        element={
          <ProtectedRoute>
            <AddCollab />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;
