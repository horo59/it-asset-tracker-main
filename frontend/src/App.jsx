
import { useCallback, useEffect, useMemo, useState } from "react";

import "./App.css";

const API_BASE_URL = "http://localhost:8083";

async function apiRequest(path, options = {}, token = null) {
  const headers = {
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  const text = await response.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const message =
      typeof data === "string"
        ? data
        : data?.message ||
          data?.error ||
          `Request failed with status ${response.status}`;

    throw new Error(message);
  }

  return data;
}

function getErrorMessage(error) {
  return error instanceof Error ? error.message : "An unexpected error occurred.";
}

function formatDate(value) {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return String(value);

  return date.toLocaleString();
}

function getCategoryName(asset, categories) {
  if (asset.categoryName) return asset.categoryName;

  if (typeof asset.category === "string") return asset.category;

  if (asset.category?.name) return asset.category.name;

  const categoryId = asset.categoryId ?? asset.category?.id;

  const category = categories.find(
    (item) => String(item.id) === String(categoryId)
  );

  return category?.name || "—";
}

function getAssignedUsername(asset) {
  return (
    asset.assignedToUsername ||
    asset.assignedTo?.username ||
    asset.assignedUser?.username ||
    asset.assignedToName ||
    "—"
  );
}

function getAssetStatus(asset) {
  return String(asset.status || "UNKNOWN").toUpperCase();
}

function App() {
  const [token, setToken] = useState(
    () => sessionStorage.getItem("accessToken") || ""
  );

  const [user, setUser] = useState(() => {
    const savedUser = sessionStorage.getItem("currentUser");

    if (!savedUser) return null;

    try {
      return JSON.parse(savedUser);
    } catch {
      return null;
    }
  });

  const [loginForm, setLoginForm] = useState({
    username: "",
    password: "",
  });

  const [assets, setAssets] = useState([]);
  const [categories, setCategories] = useState([]);

  const [assetForm, setAssetForm] = useState({
    name: "",
    serialNumber: "",
    categoryId: "",
    description: "",
  });

  const [assignForm, setAssignForm] = useState({
    assetId: "",
    userId: "",
  });

  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");

  const [selectedAsset, setSelectedAsset] = useState(null);

  const [loading, setLoading] = useState(false);
  const [loginLoading, setLoginLoading] = useState(false);
  const [addingAsset, setAddingAsset] = useState(false);
  const [assigningAsset, setAssigningAsset] = useState(false);
  const [returningAssetId, setReturningAssetId] = useState(null);
  const [deletingAssetId, setDeletingAssetId] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [detailsError, setDetailsError] = useState("");

  const isAdmin = String(user?.role || "").toUpperCase() === "ADMIN";

  const clearMessages = () => {
    setError("");
    setSuccess("");
  };

  const loadAssets = useCallback(async () => {
    if (!token) return;

    setLoading(true);

    try {
      const data = await apiRequest("/api/assets", {}, token);
      setAssets(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [token]);

  const loadCategories = useCallback(async () => {
    if (!token) return;

    try {
      const data = await apiRequest("/api/categories", {}, token);
      setCategories(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(getErrorMessage(err));
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      loadAssets();
      loadCategories();
    }
  }, [token, loadAssets, loadCategories]);

  const filteredAssets = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();

    return assets.filter((asset) => {
      const categoryName = getCategoryName(asset, categories);
      const assignedUsername = getAssignedUsername(asset);

      const matchesSearch =
        !query ||
        [
          asset.name,
          asset.serialNumber,
          categoryName,
          assignedUsername,
          asset.description,
        ].some((value) => String(value || "").toLowerCase().includes(query));

      const matchesStatus =
        statusFilter === "ALL" || getAssetStatus(asset) === statusFilter;

      const categoryId = asset.categoryId ?? asset.category?.id;

      const matchesCategory =
        categoryFilter === "ALL" ||
        String(categoryId ?? "") === categoryFilter ||
        (categoryFilter === "NO_ID" &&
          getCategoryName(asset, categories) === categoryFilter);

      return matchesSearch && matchesStatus && matchesCategory;
    });
  }, [assets, categories, searchTerm, statusFilter, categoryFilter]);

  const totalAssets = assets.length;
  const availableAssets = assets.filter(
    (asset) => getAssetStatus(asset) === "AVAILABLE"
  ).length;
  const assignedAssets = assets.filter(
    (asset) => getAssetStatus(asset) === "ASSIGNED"
  ).length;
  const maintenanceAssets = assets.filter(
    (asset) => getAssetStatus(asset) === "MAINTENANCE"
  ).length;

  async function handleLogin(event) {
    event.preventDefault();
    clearMessages();

    setLoginLoading(true);

    try {
      const data = await apiRequest("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(loginForm),
      });

      const receivedToken =
        data?.accessToken || data?.token || data?.access_token;

      if (!receivedToken) {
        throw new Error(
          "Login succeeded, but no JWT token was found in the response. Check the login response format."
        );
      }

      const loggedInUser = {
        username: data.username || loginForm.username,
        role: data.role || data.user?.role || "",
        id: data.id ?? data.user?.id ?? null,
      };

      sessionStorage.setItem("accessToken", receivedToken);
      sessionStorage.setItem("currentUser", JSON.stringify(loggedInUser));

      setToken(receivedToken);
      setUser(loggedInUser);
      setLoginForm({ username: "", password: "" });
      setSuccess("Login successful.");
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoginLoading(false);
    }
  }

  function handleLogout() {
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("currentUser");

    setToken("");
    setUser(null);
    setAssets([]);
    setCategories([]);
    setSelectedAsset(null);
    setError("");
    setSuccess("");
  }

  async function handleAddAsset(event) {
    event.preventDefault();
    clearMessages();

    if (!assetForm.name.trim() || !assetForm.serialNumber.trim()) {
      setError("Asset name and serial number are required.");
      return;
    }

    if (!assetForm.categoryId) {
      setError("Please select an asset category.");
      return;
    }

    setAddingAsset(true);

    try {
      const payload = {
        name: assetForm.name.trim(),
        serialNumber: assetForm.serialNumber.trim(),
        categoryId: Number(assetForm.categoryId),
      };

      if (assetForm.description.trim()) {
        payload.description = assetForm.description.trim();
      }

      await apiRequest(
        "/api/assets",
        {
          method: "POST",
          body: JSON.stringify(payload),
        },
        token
      );

      setAssetForm({
        name: "",
        serialNumber: "",
        categoryId: "",
        description: "",
      });

      setSuccess("Asset created successfully.");
      await loadAssets();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setAddingAsset(false);
    }
  }

  async function handleAssignAsset(event) {
    event.preventDefault();
    clearMessages();

    const assetId = assignForm.assetId;
    const userId = assignForm.userId.trim();

    if (!assetId || !userId) {
      setError("Select an asset and enter the staff user ID.");
      return;
    }

    setAssigningAsset(true);

    try {
      await apiRequest(
        `/api/assets/${assetId}/assign/${encodeURIComponent(userId)}`,
        { method: "PATCH" },
        token
      );

      setAssignForm({ assetId: "", userId: "" });
      setSuccess("Asset assigned successfully.");
      await loadAssets();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setAssigningAsset(false);
    }
  }

  async function handleReturnAsset(asset) {
    if (!window.confirm(`Return "${asset.name}" and mark it available?`)) {
      return;
    }

    clearMessages();
    setReturningAssetId(asset.id);

    try {
      await apiRequest(
        `/api/assets/${asset.id}/return`,
        { method: "PATCH" },
        token
      );

      setSuccess("Asset returned successfully.");
      await loadAssets();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setReturningAssetId(null);
    }
  }

  async function handleDeleteAsset(asset) {
    if (
      !window.confirm(
        `Delete "${asset.name}"? This action cannot be undone.`
      )
    ) {
      return;
    }

    clearMessages();
    setDeletingAssetId(asset.id);

    try {
      await apiRequest(
        `/api/assets/${asset.id}`,
        { method: "DELETE" },
        token
      );

      if (String(selectedAsset?.id) === String(asset.id)) {
        setSelectedAsset(null);
      }

      setSuccess("Asset deleted successfully.");
      await loadAssets();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDeletingAssetId(null);
    }
  }

  async function handleViewDetails(assetId) {
    setSelectedAsset(null);
    setDetailsError("");
    setDetailsLoading(true);

    try {
      const data = await apiRequest(`/api/assets/${assetId}`, {}, token);
      setSelectedAsset(data);
    } catch (err) {
      setDetailsError(getErrorMessage(err));
    } finally {
      setDetailsLoading(false);
    }
  }

  function clearFilters() {
    setSearchTerm("");
    setStatusFilter("ALL");
    setCategoryFilter("ALL");
  }

  if (!token || !user) {
    return (
      <main className="login-page">
        <section className="login-card">
          <div className="brand-mark">IT</div>
          <h1>IT Asset Tracker</h1>
          <p className="muted">Sign in to manage organizational assets.</p>

          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          <form onSubmit={handleLogin} className="form-stack">
            <label htmlFor="login-username">Username</label>
            <input
              id="login-username"
              type="text"
              autoComplete="username"
              value={loginForm.username}
              onChange={(event) =>
                setLoginForm({ ...loginForm, username: event.target.value })
              }
              placeholder="Enter your username"
              required
            />

            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              value={loginForm.password}
              onChange={(event) =>
                setLoginForm({ ...loginForm, password: event.target.value })
              }
              placeholder="Enter your password"
              required
            />

            <button className="primary-button" type="submit" disabled={loginLoading}>
              {loginLoading ? "Signing in..." : "Sign In"}
            </button>
          </form>
        </section>
      </main>
    );
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-brand">
          <div className="brand-mark small">IT</div>
          <div>
            <h1>IT Asset Tracker</h1>
            <p>Asset management dashboard</p>
          </div>
        </div>

        <div className="user-area">
          <div className="user-info">
            <strong>{user.username}</strong>
            <span>{user.role || "User"}</span>
          </div>
          <button className="secondary-button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      <main className="main-content">
        {error && (
          <div className="alert alert-error" role="alert">
            <span>{error}</span>
            <button onClick={() => setError("")} aria-label="Dismiss error">
              ×
            </button>
          </div>
        )}

        {success && (
          <div className="alert alert-success" role="status">
            <span>{success}</span>
            <button onClick={() => setSuccess("")} aria-label="Dismiss message">
              ×
            </button>
          </div>
        )}

        <section className="page-heading">
          <div>
            <p className="eyebrow">OVERVIEW</p>
            <h2>Dashboard</h2>
            <p className="muted">
              Monitor assets, assignments, and availability.
            </p>
          </div>

          <button className="secondary-button" onClick={loadAssets} disabled={loading}>
            {loading ? "Refreshing..." : "Refresh Assets"}
          </button>
        </section>

        <section className="stats-grid">
          <StatCard title="Total Assets" value={totalAssets} tone="blue" />
          <StatCard title="Available" value={availableAssets} tone="green" />
          <StatCard title="Assigned" value={assignedAssets} tone="purple" />
          <StatCard title="Maintenance" value={maintenanceAssets} tone="orange" />
        </section>

        {isAdmin && (
          <section className="management-grid">
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h3>Add Asset</h3>
                  <p className="muted">Register a new organizational asset.</p>
                </div>
              </div>

              <form onSubmit={handleAddAsset} className="form-stack">
                <label htmlFor="asset-name">Asset Name *</label>
                <input
                  id="asset-name"
                  value={assetForm.name}
                  onChange={(event) =>
                    setAssetForm({ ...assetForm, name: event.target.value })
                  }
                  placeholder="e.g. Dell Laptop"
                  required
                />

                <label htmlFor="asset-serial">Serial Number *</label>
                <input
                  id="asset-serial"
                  value={assetForm.serialNumber}
                  onChange={(event) =>
                    setAssetForm({
                      ...assetForm,
                      serialNumber: event.target.value,
                    })
                  }
                  placeholder="e.g. DELL-001"
                  required
                />

                <label htmlFor="asset-category">Category *</label>
                <select
                  id="asset-category"
                  value={assetForm.categoryId}
                  onChange={(event) =>
                    setAssetForm({ ...assetForm, categoryId: event.target.value })
                  }
                  required
                >
                  <option value="">Select category</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>

                <label htmlFor="asset-description">Description (optional)</label>
                <textarea
                  id="asset-description"
                  rows="3"
                  value={assetForm.description}
                  onChange={(event) =>
                    setAssetForm({
                      ...assetForm,
                      description: event.target.value,
                    })
                  }
                  placeholder="Additional information"
                />

                <button className="primary-button" type="submit" disabled={addingAsset}>
                  {addingAsset ? "Adding..." : "Add Asset"}
                </button>
              </form>
            </section>

            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h3>Assign Asset</h3>
                  <p className="muted">
                    Assign an available asset to a staff account.
                  </p>
                </div>
              </div>

              <form onSubmit={handleAssignAsset} className="form-stack">
                <label htmlFor="assign-asset">Available Asset *</label>
                <select
                  id="assign-asset"
                  value={assignForm.assetId}
                  onChange={(event) =>
                    setAssignForm({ ...assignForm, assetId: event.target.value })
                  }
                  required
                >
                  <option value="">Select asset</option>
                  {assets
                    .filter((asset) => getAssetStatus(asset) === "AVAILABLE")
                    .map((asset) => (
                      <option key={asset.id} value={asset.id}>
                        {asset.name} — {asset.serialNumber}
                      </option>
                    ))}
                </select>

                <label htmlFor="staff-user-id">Staff User ID *</label>
                <input
                  id="staff-user-id"
                  type="number"
                  min="1"
                  value={assignForm.userId}
                  onChange={(event) =>
                    setAssignForm({ ...assignForm, userId: event.target.value })
                  }
                  placeholder="Enter the staff user's numeric ID"
                  required
                />

                <p className="help-text">
                  Enter the ID of an existing STAFF account, not its username.
                </p>

                <button
                  className="primary-button"
                  type="submit"
                  disabled={assigningAsset}
                >
                  {assigningAsset ? "Assigning..." : "Assign Asset"}
                </button>
              </form>
            </section>
          </section>
        )}

        <section className="panel asset-panel">
          <div className="panel-heading asset-heading">
            <div>
              <h3>Assets</h3>
              <p className="muted">
                Showing {filteredAssets.length} of {assets.length} assets.
              </p>
            </div>
          </div>

          <div className="asset-filters">
            <div className="search-field">
              <label htmlFor="asset-search">Search</label>
              <input
                id="asset-search"
                type="search"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="Name, serial, category, staff..."
              />
            </div>

            <div>
              <label htmlFor="status-filter">Status</label>
              <select
                id="status-filter"
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value)}
              >
                <option value="ALL">All statuses</option>
                <option value="AVAILABLE">Available</option>
                <option value="ASSIGNED">Assigned</option>
                <option value="MAINTENANCE">Maintenance</option>
              </select>
            </div>

            <div>
              <label htmlFor="category-filter">Category</label>
              <select
                id="category-filter"
                value={categoryFilter}
                onChange={(event) => setCategoryFilter(event.target.value)}
              >
                <option value="ALL">All categories</option>
                {categories.map((category) => (
                  <option key={category.id} value={String(category.id)}>
                    {category.name}
                  </option>
                ))}
              </select>
            </div>

            <button className="secondary-button filter-clear" onClick={clearFilters}>
              Clear Filters
            </button>
          </div>

          {loading ? (
            <div className="empty-state">Loading assets...</div>
          ) : filteredAssets.length === 0 ? (
            <div className="empty-state">
              <strong>No assets found</strong>
              <p>Try changing your search or filters.</p>
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="assets-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Asset</th>
                    <th>Serial Number</th>
                    <th>Category</th>
                    <th>Status</th>
                    <th>Assigned To</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAssets.map((asset) => {
                    const status = getAssetStatus(asset);

                    return (
                      <tr key={asset.id}>
                        <td>{asset.id}</td>
                        <td>
                          <button
                            className="view-details-button"
                            onClick={() => handleViewDetails(asset.id)}
                          >
                            {asset.name}
                          </button>
                        </td>
                        <td>{asset.serialNumber || "—"}</td>
                        <td>{getCategoryName(asset, categories)}</td>
                        <td>
                          <span className={`status-badge status-${status.toLowerCase()}`}>
                            {status}
                          </span>
                        </td>
                        <td>{getAssignedUsername(asset)}</td>
                        <td>
                          <div className="asset-row-actions">
                            <button
                              className="small-button"
                              onClick={() => handleViewDetails(asset.id)}
                            >
                              View
                            </button>

                            {isAdmin && status === "ASSIGNED" && (
                              <button
                                className="small-button return-button"
                                onClick={() => handleReturnAsset(asset)}
                                disabled={returningAssetId === asset.id}
                              >
                                {returningAssetId === asset.id ? "Returning..." : "Return"}
                              </button>
                            )}

                            {isAdmin && (
                              <button
                                className="small-button delete-button"
                                onClick={() => handleDeleteAsset(asset)}
                                disabled={deletingAssetId === asset.id}
                              >
                                {deletingAssetId === asset.id ? "Deleting..." : "Delete"}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {(detailsLoading || detailsError || selectedAsset) && (
          <section className="panel asset-details">
            <div className="panel-heading">
              <div>
                <h3>Asset Details</h3>
                <p className="muted">Detailed information for the selected asset.</p>
              </div>
              <button
                className="secondary-button"
                onClick={() => {
                  setSelectedAsset(null);
                  setDetailsError("");
                }}
              >
                Close
              </button>
            </div>

            {detailsLoading && <p>Loading asset details...</p>}

            {detailsError && (
              <div className="alert alert-error">{detailsError}</div>
            )}

            {selectedAsset && (
              <dl className="details-grid">
                <Detail label="Asset ID" value={selectedAsset.id} />
                <Detail label="Name" value={selectedAsset.name} />
                <Detail label="Serial Number" value={selectedAsset.serialNumber} />
                <Detail
                  label="Category"
                  value={getCategoryName(selectedAsset, categories)}
                />
                <Detail label="Status" value={getAssetStatus(selectedAsset)} />
                <Detail
                  label="Assigned To"
                  value={getAssignedUsername(selectedAsset)}
                />
                <Detail
                  label="Description"
                  value={selectedAsset.description}
                />
                <Detail
                  label="Created At"
                  value={formatDate(selectedAsset.createdAt)}
                />
              </dl>
            )}
          </section>
        )}

        <footer className="app-footer">
          IT Asset Tracker · Asset management system
        </footer>
      </main>
    </div>
  );
}

function StatCard({ title, value, tone }) {
  return (
    <article className={`stat-card stat-${tone}`}>
      <p>{title}</p>
      <strong>{value}</strong>
    </article>
  );
}

function Detail({ label, value }) {
  return (
    <div className="detail-item">
      <dt>{label}</dt>
      <dd>{value === null || value === undefined || value === "" ? "—" : value}</dd>
    </div>
  );
}

export default App;
