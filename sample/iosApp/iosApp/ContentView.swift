import ComposeApp
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
            // The only Swift line LINE Login needs.
            //
            // When the LINE app is not installed, login happens in a web view and LINE hands
            // control back through a URL. Without this the browser closes and nothing happens —
            // the SDK never learns the login finished.
            //
            // It has to live on the SwiftUI view rather than in an AppDelegate: a SwiftUI `App` is
            // scene-based, and UIKit delivers incoming URLs to the scene, so
            // `application(_:open:options:)` is never called for an app shaped like this one.
            .onOpenURL { url in
                _ = LineLoginUrlHandler.shared.handle(url: url)
            }
    }
}
