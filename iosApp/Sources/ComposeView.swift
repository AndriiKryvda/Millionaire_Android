import SwiftUI
import Shared

/// Bridges the Compose Multiplatform UI (`MainViewController` from the `Shared` framework)
/// into the SwiftUI view hierarchy.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
